package io.waggle.waggleapiserver.domain.team.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.domain.team.dto.response.UserTeamResponse
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.request.MemberUpdateVisibilityRequest
import io.waggle.waggleapiserver.domain.user.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "팀")
@RequestMapping("/users")
@RestController
class UserTeamController(
    private val userService: UserService,
) {
    @Operation(summary = "사용자 참여 팀 목록 조회")
    @GetMapping("/{userId}/teams")
    fun getUserTeams(
        @PathVariable userId: UUID,
    ): List<UserTeamResponse> = userService.getUserTeams(userId, includeHidden = false)

    @Operation(summary = "본인 참여 팀 목록 조회")
    @GetMapping("/me/teams")
    fun getMyTeams(
        @CurrentUser user: User,
    ): List<UserTeamResponse> = userService.getUserTeams(user.id, includeHidden = true)

    @Operation(summary = "본인 팀 공개/비공개 설정")
    @PatchMapping("/me/teams/{teamId}/visibility")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateMyTeamVisibility(
        @PathVariable teamId: Long,
        @Valid @RequestBody request: MemberUpdateVisibilityRequest,
        @CurrentUser user: User,
    ) = userService.updateTeamVisibility(user.id, teamId, request)
}
