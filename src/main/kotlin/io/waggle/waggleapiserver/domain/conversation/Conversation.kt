package io.waggle.waggleapiserver.domain.conversation

import io.waggle.waggleapiserver.common.AuditingEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "conversations",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_conversations_user_partner",
            columnNames = ["user_id", "partner_id"],
        ),
    ],
    indexes = [
        Index(
            name = "idx_conversations_user_last_message",
            columnList = "user_id, last_message_id DESC",
        ),
    ],
)
class Conversation(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false)
    val userId: UUID,
    @Column(name = "partner_id", nullable = false)
    val partnerId: UUID,
    @Column(name = "last_message_id", nullable = false)
    var lastMessageId: Long,
) : AuditingEntity() {
    @Column(name = "unread_count", nullable = false)
    var unreadCount: Long = 0

    @Column(name = "last_read_message_id")
    var lastReadMessageId: Long? = null
}
