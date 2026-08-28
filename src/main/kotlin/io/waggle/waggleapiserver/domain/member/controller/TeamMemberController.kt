package io.waggle.waggleapiserver.domain.member.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.AllowIncompleteSetup
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.domain.member.dto.response.MemberResponse
import io.waggle.waggleapiserver.domain.member.service.MemberService
import io.waggle.waggleapiserver.domain.team.service.TeamService
import io.waggle.waggleapiserver.domain.user.User
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "팀 멤버")
@RequestMapping("/teams/{teamId}/members")
@RestController
class TeamMemberController(
    private val memberService: MemberService,
    private val teamService: TeamService,
) {
    @AllowIncompleteSetup
    @Operation(summary = "팀 멤버 목록 조회")
    @GetMapping
    fun getTeamMembers(
        @PathVariable teamId: Long,
        @CurrentUser user: User?,
    ): List<MemberResponse> = teamService.getTeamMembers(teamId, user)

    @Operation(
        summary = "팀 이탈",
        description = "혼자일 때는 이탈 불가, 리더일 때는 위임 후 이탈",
    )
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun leaveTeam(
        @PathVariable teamId: Long,
        @CurrentUser user: User,
    ) {
        memberService.leaveTeam(teamId, user)
    }
}
