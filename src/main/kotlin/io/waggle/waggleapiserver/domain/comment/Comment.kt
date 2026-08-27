package io.waggle.waggleapiserver.domain.comment

import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "comments",
    indexes = [
        Index(name = "idx_comments_post_parent", columnList = "post_id, parent_id"),
        Index(name = "idx_comments_user", columnList = "user_id"),
    ],
)
class Comment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "post_id", nullable = false, updatable = false)
    val postId: Long,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "parent_id", updatable = false)
    val parentId: Long? = null,
    @Column(nullable = false, columnDefinition = "VARCHAR(1000)")
    var content: String,
) : AuditingEntity() {
    @Column(name = "tombstoned_at")
    var tombstonedAt: Instant? = null

    val isReply: Boolean get() = parentId != null

    val isTombstoned: Boolean get() = tombstonedAt != null

    fun update(content: String) {
        if (isTombstoned) {
            throw BusinessException(ErrorCode.INVALID_STATE, "Cannot update deleted comment: $id")
        }
        this.content = content
    }

    fun tombstone() {
        this.tombstonedAt = Instant.now()
    }

    fun checkOwnership(currentUserId: UUID) {
        if (userId != currentUserId) {
            throw BusinessException(ErrorCode.ACCESS_DENIED, "Not the owner of the comment")
        }
    }
}
