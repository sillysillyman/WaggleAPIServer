package io.waggle.waggleapiserver.domain.team.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.member.Member
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.team.Team
import io.waggle.waggleapiserver.domain.team.enums.TeamStatus
import io.waggle.waggleapiserver.domain.team.enums.WorkMode
import io.waggle.waggleapiserver.domain.user.enums.Position
import java.time.Instant

@Schema(description = "사용자 참여 팀 응답 DTO")
data class UserTeamResponse(
    @Schema(description = "팀 ID", example = "1")
    val teamId: Long,
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
        example = "https://waggle-server.s3.ap-northeast-2.amazonaws.com/prod/teams/6df573f0-9e2e-46b5-ba7f-7d2d2873684b.png",
    )
    val profileImageUrl: String?,
    @Schema(description = "팀 멤버 수", example = "5")
    val memberCount: Int,
    @Schema(description = "팀 내 본인 포지션", example = "BACKEND")
    val position: Position,
    @Schema(description = "팀 내 본인 역할", example = "LEADER")
    val role: MemberRole,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("isVisible")
    @Schema(description = "프로필 공개 여부 (본인 조회 시에만)")
    val visible: Boolean? = null,
    @Schema(description = "팀 생성일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
    @Schema(description = "팀 수정일시", example = "2025-11-16T12:30:45.123456Z")
    val updatedAt: Instant,
) {
    companion object {
        fun of(
            team: Team,
            memberCount: Int,
            member: Member,
            visible: Boolean? = null,
        ) = UserTeamResponse(
            teamId = team.id,
            name = team.name,
            description = team.description,
            status = team.status,
            workMode = team.workMode,
            profileImageUrl = team.profileImageUrl,
            memberCount = memberCount,
            position = member.position,
            role = member.role,
            visible = visible,
            createdAt = team.createdAt,
            updatedAt = team.updatedAt,
        )
    }
}
