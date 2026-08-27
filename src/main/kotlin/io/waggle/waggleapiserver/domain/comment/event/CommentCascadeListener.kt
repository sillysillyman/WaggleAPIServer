package io.waggle.waggleapiserver.domain.comment.event

import io.waggle.waggleapiserver.domain.comment.repository.CommentRepository
import io.waggle.waggleapiserver.domain.post.event.PostDeletedEvent
import io.waggle.waggleapiserver.domain.team.event.TeamDeletedEvent
import io.waggle.waggleapiserver.domain.user.event.UserDeactivatedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class CommentCascadeListener(
    private val commentRepository: CommentRepository,
) {
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onPostDeleted(event: PostDeletedEvent) {
        commentRepository.updateDeletedAtByPostIdAndDeletedAtIsNull(event.postId)
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onTeamDeleted(event: TeamDeletedEvent) {
        commentRepository.updateDeletedAtByPostInTeamIdAndDeletedAtIsNull(event.teamId)
    }

    // 탈퇴 cascade만 tombstone을 쓰는 이유: 남의 답글이 부모를 잃으면 고아가 됨.
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    fun onUserDeactivated(event: UserDeactivatedEvent) {
        commentRepository.updateTombstonedAtByUserIdAndHasReply(event.userId)
        commentRepository.updateDeletedAtByUserIdAndHasNoReply(event.userId)
        commentRepository.updateDeletedAtByEmptiedTombstoneParentOfUserId(event.userId)
    }
}
