package io.waggle.waggleapiserver.domain.notification.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.notification.Notification
import io.waggle.waggleapiserver.domain.notification.NotificationType
import io.waggle.waggleapiserver.domain.team.dto.response.NotificationTeamResponse
import io.waggle.waggleapiserver.domain.user.User
import java.time.Instant
import java.util.UUID

@Schema(description = "알림 응답 DTO")
data class NotificationResponse(
    @Schema(description = "알림 ID", example = "1")
    val notificationId: Long,
    @Schema(description = "알림 타입", example = "APPLICATION_RECEIVED")
    val type: NotificationType,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "팀 정보")
    val team: NotificationTeamResponse?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "관련 사용자 정보")
    val triggeredBy: TriggeredByResponse?,
    @Schema(description = "알림 확인일시")
    val readAt: Instant?,
    @Schema(description = "알림 생성일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
) {
    @Schema(description = "관련 사용자 정보")
    data class TriggeredByResponse(
        @Schema(description = "사용자 ID")
        val userId: UUID,
        @Schema(description = "사용자명")
        val username: String?,
    ) {
        companion object {
            fun of(user: User) =
                TriggeredByResponse(
                    userId = user.id,
                    username = user.username,
                )
        }
    }

    companion object {
        fun of(
            notification: Notification,
            team: NotificationTeamResponse?,
            triggeredBy: TriggeredByResponse?,
        ): NotificationResponse =
            NotificationResponse(
                notificationId = notification.id,
                type = notification.type,
                team = team,
                triggeredBy =
                    when (notification.type) {
                        NotificationType.APPLICATION_RECEIVED,
                        NotificationType.APPLICATION_REMIND,
                        NotificationType.MEMBER_JOINED,
                        NotificationType.MEMBER_LEFT,
                        -> triggeredBy

                        else -> null
                    },
                readAt = notification.readAt,
                createdAt = notification.createdAt,
            )
    }
}
