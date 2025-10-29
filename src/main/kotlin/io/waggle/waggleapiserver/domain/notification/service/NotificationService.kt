package io.waggle.waggleapiserver.domain.notification.service

import io.waggle.waggleapiserver.domain.notification.Notification
import io.waggle.waggleapiserver.domain.notification.dto.request.NotificationCreateRequest
import io.waggle.waggleapiserver.domain.notification.dto.response.NotificationResponse
import io.waggle.waggleapiserver.domain.notification.repository.NotificationRepository
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun createNotification(
        userId: UUID,
        request: NotificationCreateRequest,
    ) {
        if (!userRepository.existsById(userId)) {
            throw EntityNotFoundException("User not found: $userId")
        }

        val (type, redirectUrl, contentArgs) = request
        val title = type.title
        val content = type.content(*contentArgs.toTypedArray())

        val notification =
            Notification(
                title = title,
                content = content,
                redirectUrl = redirectUrl,
                userId = userId,
            )

        notificationRepository.save(notification)
    }

    fun getUserNotifications(userId: UUID): List<NotificationResponse> {
        val notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
        return notifications.map { NotificationResponse.from(it) }
    }
}
