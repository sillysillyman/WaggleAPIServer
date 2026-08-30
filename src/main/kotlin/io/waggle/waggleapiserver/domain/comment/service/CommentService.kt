package io.waggle.waggleapiserver.domain.comment.service

import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.dto.response.CursorResponse
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.comment.Comment
import io.waggle.waggleapiserver.domain.comment.dto.request.CommentCreateRequest
import io.waggle.waggleapiserver.domain.comment.dto.request.CommentUpdateRequest
import io.waggle.waggleapiserver.domain.comment.dto.response.CommentResponse
import io.waggle.waggleapiserver.domain.comment.event.CommentDeletedEvent
import io.waggle.waggleapiserver.domain.comment.repository.CommentRepository
import io.waggle.waggleapiserver.domain.like.LikeId
import io.waggle.waggleapiserver.domain.like.LikeType
import io.waggle.waggleapiserver.domain.like.repository.LikeRepository
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CommentService(
    private val eventPublisher: ApplicationEventPublisher,
    private val commentRepository: CommentRepository,
    private val likeRepository: LikeRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun createComment(
        postId: Long,
        request: CommentCreateRequest,
        user: User,
    ): CommentResponse {
        val (parentId, content) = request

        postRepository.findByIdOrNull(postId)
            ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Post not found: $postId")

        parentId?.let { getParentComment(it, postId) }

        val savedComment =
            commentRepository.save(
                Comment(
                    postId = postId,
                    userId = user.id,
                    parentId = parentId,
                    content = content,
                ),
            )

        return CommentResponse.of(savedComment, UserSimpleResponse.from(user))
    }

    fun getComments(
        postId: Long,
        cursorQuery: CursorGetQuery,
        user: User?,
    ): CursorResponse<CommentResponse> {
        val roots =
            commentRepository.findRootsByPostIdWithCursor(
                postId = postId,
                cursor = cursorQuery.cursor,
                pageable = PageRequest.of(0, cursorQuery.size + 1),
            )

        val hasNext = roots.size > cursorQuery.size
        val content = if (hasNext) roots.take(cursorQuery.size) else roots
        val nextCursor = if (hasNext) content.last().id else null

        val replies =
            if (content.isEmpty()) {
                emptyList()
            } else {
                commentRepository.findByPostIdAndParentIdInOrderByParentIdAscIdAsc(
                    postId,
                    content.map { it.id },
                )
            }
        val repliesByParentId = replies.groupBy { it.parentId }

        val comments = content + replies
        val authorById =
            userRepository.findAllById(comments.map { it.userId }.distinct()).associateBy { it.id }

        // 탈퇴자의 댓글은 tombstone으로 남지만 user 행은 soft delete되어 여기 안 잡힌다.
        comments
            .firstOrNull { it.userId !in authorById && !it.isTombstoned }
            ?.let {
                throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "User not found: ${it.userId}",
                )
            }

        // 답글에도 좋아요가 달리므로 최상위 댓글과 답글 id 전부가 배치 조회 대상.
        val commentIds = comments.map { it.id }
        val likeCountByCommentId =
            if (commentIds.isEmpty()) {
                emptyMap()
            } else {
                likeRepository
                    .countLikesGroupByTargetId(LikeType.COMMENT, commentIds)
                    .associate { it.targetId to it.likeCount }
            }
        val likedCommentIdSet =
            if (user == null || commentIds.isEmpty()) {
                emptySet()
            } else {
                likeRepository
                    .findTargetIdsByUserIdAndTypeAndTargetIdIn(user.id, LikeType.COMMENT, commentIds)
                    .toSet()
            }

        val data =
            content.map {
                CommentResponse.of(
                    it,
                    repliesByParentId[it.id] ?: emptyList(),
                    authorById,
                    likeCountByCommentId,
                    likedCommentIdSet,
                )
            }

        return CursorResponse(
            data = data,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    @Transactional
    fun updateComment(
        commentId: Long,
        request: CommentUpdateRequest,
        user: User,
    ): CommentResponse {
        val comment =
            commentRepository.findByIdOrNull(commentId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Comment not found: $commentId",
                )
        comment.checkOwnership(user.id)
        comment.update(request.content)

        return CommentResponse.of(
            comment,
            UserSimpleResponse.from(user),
            likeRepository.countByIdTypeAndIdTargetId(LikeType.COMMENT, commentId),
            likeRepository.existsById(LikeId(LikeType.COMMENT, commentId, user.id)),
        )
    }

    @Transactional
    fun deleteComment(
        commentId: Long,
        user: User,
    ) {
        val comment =
            commentRepository.findWithLockById(commentId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Comment not found: $commentId",
                )
        comment.checkOwnership(user.id)

        // tombstone도 본문이 사라지므로 좋아요 정리 대상. 분기 이전에 한 번만 발행.
        eventPublisher.publishEvent(CommentDeletedEvent(commentId))

        if (comment.isReply) {
            comment.delete()
            cleanUpTombstonedParent(comment)
            return
        }

        if (commentRepository.existsByPostIdAndParentId(comment.postId, commentId)) {
            comment.tombstone()
        } else {
            comment.delete()
        }
    }

    private fun getParentComment(
        parentId: Long,
        postId: Long,
    ): Comment {
        val parent =
            commentRepository.findWithLockById(parentId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Comment not found: $parentId",
                )
        if (parent.postId != postId) {
            throw BusinessException(
                ErrorCode.INVALID_STATE,
                "Comment does not belong to post: $parentId, $postId",
            )
        }
        if (parent.isReply) {
            throw BusinessException(ErrorCode.INVALID_STATE, "Cannot reply to a reply: $parentId")
        }
        return parent
    }

    private fun cleanUpTombstonedParent(reply: Comment) {
        val parentId = reply.parentId ?: return
        val parent = commentRepository.findWithLockById(parentId) ?: return
        if (!parent.isTombstoned) {
            return
        }
        if (commentRepository.existsByPostIdAndParentId(parent.postId, parentId)) {
            return
        }
        parent.delete()
    }
}
