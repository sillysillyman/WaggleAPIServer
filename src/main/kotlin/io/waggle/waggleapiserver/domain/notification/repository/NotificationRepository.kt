package io.waggle.waggleapiserver.domain.notification.repository

import io.waggle.waggleapiserver.domain.notification.Notification
import io.waggle.waggleapiserver.domain.notification.NotificationType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun countByUserId(userId: UUID): Long

    fun countByUserIdAndReadAtIsNull(userId: UUID): Long

    @Query(
        """
        SELECT n FROM Notification n
        WHERE n.userId = :userId
        AND (:cursor IS NULL OR n.id < :cursor)
        ORDER BY n.id DESC
        """,
    )
    fun findByUserIdWithCursor(
        userId: UUID,
        cursor: Long?,
        pageable: Pageable,
    ): List<Notification>

    @Query(
        """
        SELECT n FROM Notification n
        WHERE n.userId IN :userIds
        AND n.type = :type
        AND n.createdAt >= :since
        """,
    )
    fun findByUserIdInAndTypeAndCreatedAtAfter(
        userIds: List<UUID>,
        type: NotificationType,
        since: Instant,
    ): List<Notification>

    @Modifying
    @Query(
        """
        UPDATE notifications SET read_at = UTC_TIMESTAMP(6)
        WHERE user_id = :userId AND id IN :ids AND read_at IS NULL
        """,
        nativeQuery = true,
    )
    fun markAsReadByIds(
        userId: UUID,
        ids: List<Long>,
    )

    @Modifying
    @Query(
        """
        UPDATE notifications SET read_at = UTC_TIMESTAMP(6)
        WHERE user_id = :userId AND read_at IS NULL
        """,
        nativeQuery = true,
    )
    fun markAllAsRead(userId: UUID)

    fun deleteByUserId(userId: UUID)

    @Modifying
    @Query(
        """
        DELETE FROM notifications
        WHERE CAST(JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.teamId')) AS UNSIGNED) = :teamId
        """,
        nativeQuery = true,
    )
    fun deleteByMetadataTeamId(teamId: Long)

    @Modifying
    @Query(
        """
        DELETE FROM notifications
        WHERE CAST(JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.postId')) AS UNSIGNED) = :postId
        """,
        nativeQuery = true,
    )
    fun deleteByMetadataPostId(postId: Long)

    @Modifying
    @Query(
        """
        DELETE n FROM notifications n
        JOIN posts p ON CAST(JSON_UNQUOTE(JSON_EXTRACT(n.metadata, '$.postId')) AS UNSIGNED) = p.id
        WHERE p.team_id = :teamId
        """,
        nativeQuery = true,
    )
    fun deleteByMetadataPostInTeamId(teamId: Long)

    @Modifying
    @Query(
        """
        DELETE n FROM notifications n
        JOIN posts p ON CAST(JSON_UNQUOTE(JSON_EXTRACT(n.metadata, '$.postId')) AS UNSIGNED) = p.id
        WHERE p.user_id = :userId
        """,
        nativeQuery = true,
    )
    fun deleteByMetadataPostUserId(userId: UUID)
}
