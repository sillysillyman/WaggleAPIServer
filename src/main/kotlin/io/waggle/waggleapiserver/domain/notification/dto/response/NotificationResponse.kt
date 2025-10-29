package io.waggle.waggleapiserver.domain.notification.dto.response

import io.waggle.waggleapiserver.domain.notification.Notification
import java.time.Instant

data class NotificationResponse(
    val notificationId: Long,
    val title: String,
    val content: String,
    val redirectUrl: String,
    val isRead: Boolean,
    val createdAt: Instant,
) {
    companion object {
        fun from(notification: Notification): NotificationResponse =
            NotificationResponse(
                notification.id,
                notification.title,
                notification.content,
                notification.redirectUrl,
                notification.isRead,
                notification.createdAt,
            )
    }
}
