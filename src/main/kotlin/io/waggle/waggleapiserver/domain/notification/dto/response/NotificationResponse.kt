package io.waggle.waggleapiserver.domain.notification.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.notification.Notification
import io.waggle.waggleapiserver.domain.notification.NotificationType
import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.team.Team
import io.waggle.waggleapiserver.domain.user.User
import java.time.Instant
import java.util.UUID

@Schema(description = "알림 응답 DTO")
data class NotificationResponse(
    @Schema(description = "알림 ID", example = "1")
    val id: Long,
    @Schema(description = "알림 타입", example = "APPLICATION_RECEIVED")
    val type: NotificationType,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "타입별 부가 정보 (team, triggeredBy, post 등)")
    val metadata: Map<String, Any?>?,
    @Schema(description = "알림 확인일시")
    val readAt: Instant?,
    @Schema(description = "알림 생성일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
) {
    @Schema(description = "팀 정보")
    data class TeamResponse(
        @Schema(description = "팀 ID", example = "1")
        val id: Long,
        @Schema(description = "팀명", example = "Waggle")
        val name: String,
        @Schema(
            description = "프로필 이미지 URL",
            example = "https://example.com.png",
        )
        val profileImageUrl: String?,
    ) {
        companion object {
            fun from(team: Team) =
                TeamResponse(
                    id = team.id,
                    name = team.name,
                    profileImageUrl = team.profileImageUrl,
                )
        }
    }

    @Schema(description = "모집글 정보")
    data class PostResponse(
        @Schema(description = "모집글 ID", example = "1")
        val id: Long,
        @Schema(description = "모집글 제목", example = "백엔드 개발자 모집")
        val title: String,
    ) {
        companion object {
            fun from(post: Post) =
                PostResponse(
                    id = post.id,
                    title = post.title,
                )
        }
    }

    @Schema(description = "관련 사용자 정보")
    data class TriggeredByResponse(
        @Schema(description = "사용자 ID")
        val id: UUID,
        @Schema(description = "사용자명")
        val username: String?,
    ) {
        companion object {
            fun from(user: User) =
                TriggeredByResponse(
                    id = user.id,
                    username = user.username,
                )
        }
    }

    companion object {
        fun of(
            notification: Notification,
            metadata: Map<String, Any?>?,
        ): NotificationResponse =
            NotificationResponse(
                id = notification.id,
                type = notification.type,
                metadata = metadata,
                readAt = notification.readAt,
                createdAt = notification.createdAt,
            )
    }
}
