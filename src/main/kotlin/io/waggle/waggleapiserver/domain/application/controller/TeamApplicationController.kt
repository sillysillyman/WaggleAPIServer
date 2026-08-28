package io.waggle.waggleapiserver.domain.application.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.dto.response.CursorResponse
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.domain.application.dto.request.ApplicationCreateRequest
import io.waggle.waggleapiserver.domain.application.dto.response.ApplicationResponse
import io.waggle.waggleapiserver.domain.application.dto.response.TeamApplicationResponse
import io.waggle.waggleapiserver.domain.application.service.ApplicationService
import io.waggle.waggleapiserver.domain.user.User
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "팀 지원")
@RequestMapping("/teams/{teamId}/applications")
@RestController
class TeamApplicationController(
    private val applicationService: ApplicationService,
) {
    @Operation(
        summary = "팀 지원",
        description = "사용자가 해당 팀 합류를 지원함",
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun applyToTeam(
        @PathVariable teamId: Long,
        @Valid @RequestBody request: ApplicationCreateRequest,
        @CurrentUser user: User,
    ): ApplicationResponse = applicationService.applyToTeam(teamId, request, user)

    @Operation(
        summary = "팀 지원 목록 조회",
        description = "팀 멤버 권한 사용자가 팀 지원 목록을 조회함",
    )
    @GetMapping
    fun getTeamApplications(
        @PathVariable teamId: Long,
        @RequestParam(required = false) postId: Long?,
        @Valid @ParameterObject cursorQuery: CursorGetQuery,
        @CurrentUser user: User,
    ): CursorResponse<TeamApplicationResponse> =
        applicationService.getTeamApplications(teamId, postId, cursorQuery, user)
}
