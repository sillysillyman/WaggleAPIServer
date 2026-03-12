package io.waggle.waggleapiserver.domain.notification.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.notification.Notification
import io.waggle.waggleapiserver.domain.notification.NotificationType
import io.waggle.waggleapiserver.domain.team.dto.response.TeamResponse
import java.time.Instant

@Schema(description = "알림 응답 DTO")
data class NotificationResponse(
    @Schema(description = "알림 ID", example = "1")
    val notificationId: Long,
    @Schema(description = "알림 타입", example = "APPLICATION_RECEIVED")
    val type: NotificationType,
    @Schema(description = "팀 정보")
    val team: TeamResponse?,
    @Schema(description = "알림 확인일시", example = "false")
    val readAt: Instant?,
    @Schema(description = "알림 생성일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
) {
    companion object {
        fun of(
            notification: Notification,
            team: TeamResponse?,
        ): NotificationResponse =
            NotificationResponse(
                notificationId = notification.id,
                type = notification.type,
                team = team,
                readAt = notification.readAt,
                createdAt = notification.createdAt,
            )
    }
}
