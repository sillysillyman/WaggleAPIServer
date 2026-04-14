package io.waggle.waggleapiserver.domain.message.repository

import io.waggle.waggleapiserver.domain.message.Message
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface MessageRepository : JpaRepository<Message, Long> {
    @Query(
        """
        SELECT MAX(m.id) FROM Message m
        WHERE m.senderId = :senderId AND m.receiverId = :receiverId
        AND m.readAt IS NOT NULL
        """,
    )
    fun findLastReadMessageId(
        senderId: UUID,
        receiverId: UUID,
    ): Long?

    @Query(
        """
        SELECT m FROM Message m
        WHERE ((m.senderId = :userId AND m.receiverId = :partnerId)
            OR (m.senderId = :partnerId AND m.receiverId = :userId))
        AND (:cursor IS NULL OR m.id < :cursor)
        ORDER BY m.id DESC
        """,
    )
    fun findMessageHistoryBefore(
        userId: UUID,
        partnerId: UUID,
        cursor: Long?,
        pageable: Pageable,
    ): List<Message>

    @Query(
        """
        SELECT m FROM Message m
        WHERE ((m.senderId = :userId AND m.receiverId = :partnerId)
            OR (m.senderId = :partnerId AND m.receiverId = :userId))
        AND (:cursor IS NULL OR m.id > :cursor)
        ORDER BY m.id ASC
        """,
    )
    fun findMessageHistoryAfter(
        userId: UUID,
        partnerId: UUID,
        cursor: Long?,
        pageable: Pageable,
    ): List<Message>

    @Query(
        value = """
            SELECT m.* FROM messages m
            INNER JOIN (
                SELECT MAX(m2.id) AS max_id
                FROM messages m2
                WHERE (m2.sender_id = :userId OR m2.receiver_id = :userId)
                AND MATCH(m2.content) AGAINST(:q IN BOOLEAN MODE)
                GROUP BY IF(m2.sender_id = :userId, m2.receiver_id, m2.sender_id)
            ) latest ON m.id = latest.max_id
            ORDER BY m.id DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun searchByContent(
        userId: UUID,
        q: String,
        limit: Int,
    ): List<Message>

    @Modifying
    @Query(
        """
        UPDATE messages SET read_at = UTC_TIMESTAMP(6)
        WHERE sender_id = :senderId AND receiver_id = :receiverId
        AND read_at IS NULL
        """,
        nativeQuery = true,
    )
    fun markAllAsRead(
        senderId: UUID,
        receiverId: UUID,
    ): Int
}
