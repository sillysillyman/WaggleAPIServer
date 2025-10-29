package io.waggle.waggleapiserver.domain.notification.repository

import io.waggle.waggleapiserver.domain.notification.Notification
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<Notification>
}
