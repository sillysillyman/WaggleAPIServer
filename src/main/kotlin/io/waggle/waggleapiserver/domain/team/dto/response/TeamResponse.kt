package io.waggle.waggleapiserver.domain.team.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkResponse
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.team.Team
import io.waggle.waggleapiserver.domain.team.enums.TeamStatus
import io.waggle.waggleapiserver.domain.team.enums.WorkMode
import java.time.Instant

@Schema(description = "팀 응답 DTO")
data class TeamResponse(
    @Schema(description = "팀 ID", example = "1")
    val id: Long,
    @Schema(description = "팀명", example = "Waggle")
    val name: String,
    @Schema(description = "팀 설명")
    val description: String,
    @Schema(description = "팀 상태", example = "ACTIVE")
    val status: TeamStatus,
    @Schema(description = "진행 방식", example = "ONLINE")
    val workMode: WorkMode,
    @Schema(
        description = "프로필 이미지 URL",
        example = "https://example.com.png",
    )
    val profileImageUrl: String?,
    @Schema(description = "팀 멤버 수", example = "5")
    val memberCount: Int,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "현재 사용자의 팀 내 역할 (멤버가 아니면 응답에서 제외)", example = "LEADER")
    val memberRole: MemberRole? = null,
    @Schema(description = "팀 생성일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
    @Schema(description = "팀 수정일시", example = "2025-11-16T12:30:45.123456Z")
    val updatedAt: Instant,
) : BookmarkResponse {
    companion object {
        fun of(
            team: Team,
            memberCount: Int,
            memberRole: MemberRole?,
        ) = TeamResponse(
            id = team.id,
            name = team.name,
            description = team.description,
            status = team.status,
            workMode = team.workMode,
            profileImageUrl = team.profileImageUrl,
            memberCount = memberCount,
            memberRole = memberRole,
            createdAt = team.createdAt,
            updatedAt = team.updatedAt,
        )
    }
}
