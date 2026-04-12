package io.waggle.waggleapiserver.domain.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.application.Application
import io.waggle.waggleapiserver.domain.application.ApplicationStatus
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.enums.Position
import java.time.Instant
import java.util.UUID

@Schema(description = "팀 지원 응답 DTO")
data class TeamApplicationResponse(
    @Schema(description = "지원 ID", example = "1")
    val applicationId: Long,
    @Schema(description = "지원 직무", example = "BACKEND")
    val position: Position,
    @Schema(description = "지원 상태", example = "APPROVED")
    val status: ApplicationStatus,
    @Schema(description = "지원 팀 ID", example = "1")
    val teamId: Long,
    @Schema(description = "모집글 ID", example = "1")
    val postId: Long,
    @Schema(description = "지원자 정보")
    val user: ApplicantResponse,
    @Schema(description = "읽음 여부", example = "true")
    val isRead: Boolean,
    @Schema(description = "지원 동기")
    val detail: String?,
    @Schema(
        description = "포트폴리오 URL 목록",
        example = "[\"https://github.com/user\", \"https://blog.example.com\"]",
    )
    val portfolioUrls: List<String>,
    @Schema(description = "지원일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
    @Schema(description = "지원 수정일시", example = "2025-11-16T12:30:45.123456Z")
    val updatedAt: Instant,
) {
    @Schema(description = "지원자 정보")
    data class ApplicantResponse(
        @Schema(description = "사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        val userId: UUID,
        @Schema(description = "사용자명", example = "testUser")
        val username: String,
        @Schema(description = "협업 온도", example = "36.5")
        val temperature: Double,
        @Schema(
            description = "프로필 이미지 URL",
            example = "https://avatars.githubusercontent.com/u/112466204?s=80&v=4",
        )
        val profileImageUrl: String?,
    ) {
        companion object {
            fun from(user: User): ApplicantResponse =
                ApplicantResponse(
                    userId = user.id,
                    username = user.username!!,
                    temperature = user.temperature,
                    profileImageUrl = user.profileImageUrl,
                )
        }
    }

    companion object {
        fun of(
            application: Application,
            user: User,
            isRead: Boolean,
        ): TeamApplicationResponse =
            TeamApplicationResponse(
                applicationId = application.id,
                position = application.position,
                status = application.status,
                teamId = application.teamId,
                postId = application.postId,
                user = ApplicantResponse.from(user),
                isRead = isRead,
                detail = application.detail,
                portfolioUrls = application.portfolioUrls.toList(),
                createdAt = application.createdAt,
                updatedAt = application.updatedAt,
            )
    }
}
