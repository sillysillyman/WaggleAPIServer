package io.waggle.waggleapiserver.domain.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.application.Application
import io.waggle.waggleapiserver.domain.application.ApplicationStatus
import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.team.Team
import io.waggle.waggleapiserver.domain.user.enums.Position
import java.time.Instant

@Schema(description = "본인 지원 응답 DTO")
data class UserApplicationResponse(
    @Schema(description = "지원 ID", example = "1")
    val id: Long,
    @Schema(description = "지원 직무", example = "BACKEND")
    val position: Position,
    @Schema(description = "지원 상태", example = "PENDING")
    val status: ApplicationStatus,
    @Schema(description = "지원 팀 정보")
    val team: TeamResponse,
    @Schema(description = "지원 모집글 정보")
    val post: PostResponse,
    @Schema(description = "지원 동기")
    val detail: String?,
    @Schema(
        description = "포트폴리오 URL 목록",
        example = "[\"https://github.com/user\", \"https://blog.example.com\"]",
    )
    val portfolioUrls: List<String>,
    @Schema(description = "지원일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
) {
    @Schema(description = "팀 정보")
    data class TeamResponse(
        @Schema(description = "팀 ID", example = "1")
        val id: Long,
        @Schema(description = "팀명", example = "Waggle")
        val name: String,
    ) {
        companion object {
            fun from(team: Team): TeamResponse = TeamResponse(id = team.id, name = team.name)
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
            fun from(post: Post): PostResponse = PostResponse(id = post.id, title = post.title)
        }
    }

    companion object {
        fun of(
            application: Application,
            team: Team,
            post: Post,
        ): UserApplicationResponse =
            UserApplicationResponse(
                id = application.id,
                position = application.position,
                status = application.status,
                team = TeamResponse.from(team),
                post = PostResponse.from(post),
                detail = application.detail,
                portfolioUrls = application.portfolioUrls.toList(),
                createdAt = application.createdAt,
            )
    }
}
