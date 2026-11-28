package io.waggle.waggleapiserver.domain.message.repository

import io.waggle.waggleapiserver.domain.message.Message
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

    // 두 방향을 OR로 묶으면 PK 순으로 훑어 오래된 대화일수록 다른 대화의 메시지까지 읽음
    // FORCE INDEX가 없으면 커서가 있을 때 두 인덱스의 교집합으로 대화 전체를 읽고 정렬함
    @Query(
        """
        SELECT * FROM (
            (SELECT * FROM messages FORCE INDEX (idx_messages_sender_receiver)
            WHERE sender_id = :userId AND receiver_id = :partnerId AND deleted_at IS NULL
            AND (:cursor IS NULL OR id < :cursor)
            ORDER BY id DESC LIMIT :limit)
            UNION ALL
            (SELECT * FROM messages FORCE INDEX (idx_messages_sender_receiver)
            WHERE sender_id = :partnerId AND receiver_id = :userId AND deleted_at IS NULL
            AND (:cursor IS NULL OR id < :cursor)
            ORDER BY id DESC LIMIT :limit)
        ) m
        ORDER BY id DESC
        LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findMessageHistoryBefore(
        userId: UUID,
        partnerId: UUID,
        cursor: Long?,
        limit: Int,
    ): List<Message>

    @Query(
        """
        SELECT * FROM (
            (SELECT * FROM messages FORCE INDEX (idx_messages_sender_receiver)
            WHERE sender_id = :userId AND receiver_id = :partnerId AND deleted_at IS NULL
            AND (:cursor IS NULL OR id > :cursor)
            ORDER BY id ASC LIMIT :limit)
            UNION ALL
            (SELECT * FROM messages FORCE INDEX (idx_messages_sender_receiver)
            WHERE sender_id = :partnerId AND receiver_id = :userId AND deleted_at IS NULL
            AND (:cursor IS NULL OR id > :cursor)
            ORDER BY id ASC LIMIT :limit)
        ) m
        ORDER BY id ASC
        LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findMessageHistoryAfter(
        userId: UUID,
        partnerId: UUID,
        cursor: Long?,
        limit: Int,
    ): List<Message>

    @Query(
        """
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
