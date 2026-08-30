package io.waggle.waggleapiserver.domain.like.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.comment.repository.CommentRepository
import io.waggle.waggleapiserver.domain.like.Like
import io.waggle.waggleapiserver.domain.like.LikeId
import io.waggle.waggleapiserver.domain.like.LikeType
import io.waggle.waggleapiserver.domain.like.dto.response.LikeResponse
import io.waggle.waggleapiserver.domain.like.repository.LikeRepository
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.user.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class LikeService(
    private val commentRepository: CommentRepository,
    private val likeRepository: LikeRepository,
    private val postRepository: PostRepository,
) {
    @Transactional
    fun like(
        type: LikeType,
        targetId: Long,
        user: User,
    ): LikeResponse {
        checkTargetExists(type, targetId)

        val likeId = LikeId(type = type, targetId = targetId, userId = user.id)
        if (!likeRepository.existsById(likeId)) {
            likeRepository.save(Like(likeId))
        }

        return LikeResponse.of(
            liked = true,
            likeCount = likeRepository.countByIdTypeAndIdTargetId(type, targetId),
        )
    }

    @Transactional
    fun unlike(
        type: LikeType,
        targetId: Long,
        user: User,
    ): LikeResponse {
        checkTargetExists(type, targetId)

        likeRepository.deleteById(LikeId(type = type, targetId = targetId, userId = user.id))

        return LikeResponse.of(
            liked = false,
            likeCount = likeRepository.countByIdTypeAndIdTargetId(type, targetId),
        )
    }

    // tombstone은 soft delete가 아니라 @SQLRestriction에 안 걸리므로 조건을 명시함.
    private fun checkTargetExists(
        type: LikeType,
        targetId: Long,
    ) {
        val exists =
            when (type) {
                LikeType.POST -> postRepository.existsById(targetId)
                LikeType.COMMENT -> commentRepository.existsByIdAndTombstonedAtIsNull(targetId)
            }
        if (!exists) {
            throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "$type not found: $targetId")
        }
    }
}
