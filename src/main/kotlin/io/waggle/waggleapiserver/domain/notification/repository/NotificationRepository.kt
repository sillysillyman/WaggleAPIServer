package io.waggle.waggleapiserver.domain.notification.repository

import io.waggle.waggleapiserver.domain.notification.Notification
import io.waggle.waggleapiserver.domain.notification.NotificationType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByApplicationIdInAndType(
        applicationIds: List<Long>,
        type: NotificationType,
    ): List<Notification>

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
}
