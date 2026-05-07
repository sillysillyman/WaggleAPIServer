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
    val applicationId: Long,
    @Schema(description = "지원 직무", example = "BACKEND")
    val position: Position,
    @Schema(description = "지원 상태", example = "PENDING")
    val status: ApplicationStatus,
    @Schema(description = "지원 팀 정보")
    val team: TeamSummary,
    @Schema(description = "지원 모집글 정보")
    val post: PostSummary,
    @Schema(description = "지원일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
) {
    @Schema(description = "지원 팀 요약")
    data class TeamSummary(
        @Schema(description = "팀 ID", example = "1")
        val teamId: Long,
        @Schema(description = "팀 이름", example = "와글와글")
        val name: String,
    ) {
        companion object {
            fun from(team: Team): TeamSummary = TeamSummary(teamId = team.id, name = team.name)
        }
    }

    @Schema(description = "지원 모집글 요약")
    data class PostSummary(
        @Schema(description = "모집글 ID", example = "1")
        val postId: Long,
        @Schema(description = "모집글 제목", example = "Flutter 개발자를 모십니다")
        val title: String,
    ) {
        companion object {
            fun from(post: Post): PostSummary = PostSummary(postId = post.id, title = post.title)
        }
    }

    companion object {
        fun of(
            application: Application,
            team: Team,
            post: Post,
        ): UserApplicationResponse =
            UserApplicationResponse(
                applicationId = application.id,
                position = application.position,
                status = application.status,
                team = TeamSummary.from(team),
                post = PostSummary.from(post),
                createdAt = application.createdAt,
            )
    }
}
