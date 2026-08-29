package io.waggle.waggleapiserver.domain.like.event

import io.waggle.waggleapiserver.domain.comment.event.CommentDeletedEvent
import io.waggle.waggleapiserver.domain.like.LikeType
import io.waggle.waggleapiserver.domain.like.repository.LikeRepository
import io.waggle.waggleapiserver.domain.post.event.PostDeletedEvent
import io.waggle.waggleapiserver.domain.team.event.TeamDeletedEvent
import io.waggle.waggleapiserver.domain.user.event.UserDeactivatedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class LikeCascadeListener(
    private val likeRepository: LikeRepository,
) {
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onCommentDeleted(event: CommentDeletedEvent) {
        likeRepository.deleteByIdTypeAndIdTargetId(LikeType.COMMENT, event.commentId)
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onPostDeleted(event: PostDeletedEvent) {
        likeRepository.deleteByIdTypeAndIdTargetId(LikeType.POST, event.postId)
        likeRepository.deleteByCommentPostId(event.postId)
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onTeamDeleted(event: TeamDeletedEvent) {
        likeRepository.deleteByPostTeamId(event.teamId)
        likeRepository.deleteByCommentPostTeamId(event.teamId)
    }

    // 그 사용자가 누른 좋아요와 그 사용자의 글·댓글이 받은 좋아요를 모두 정리.
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onUserDeactivated(event: UserDeactivatedEvent) {
        likeRepository.deleteByIdUserId(event.userId)
        likeRepository.deleteByPostUserId(event.userId)
        likeRepository.deleteByCommentUserId(event.userId)
    }
}
