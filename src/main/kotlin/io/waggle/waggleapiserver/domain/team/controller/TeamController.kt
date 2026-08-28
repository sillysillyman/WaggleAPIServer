package io.waggle.waggleapiserver.domain.team.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.AllowIncompleteSetup
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.common.infrastructure.persistence.RequireCompleteSetup
import io.waggle.waggleapiserver.common.storage.dto.request.PresignedUrlRequest
import io.waggle.waggleapiserver.common.storage.dto.response.PresignedUrlResponse
import io.waggle.waggleapiserver.domain.team.dto.request.TeamStatusUpdateRequest
import io.waggle.waggleapiserver.domain.team.dto.request.TeamUpsertRequest
import io.waggle.waggleapiserver.domain.team.dto.response.TeamResponse
import io.waggle.waggleapiserver.domain.team.service.TeamService
import io.waggle.waggleapiserver.domain.user.User
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "팀")
@RequestMapping("/teams")
@RestController
class TeamController(
    private val teamService: TeamService,
) {
    @Operation(summary = "팀 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTeam(
        @Valid @RequestBody request: TeamUpsertRequest,
        @CurrentUser user: User,
    ): TeamResponse = teamService.createTeam(request, user)

    @RequireCompleteSetup
    @Operation(summary = "팀 프로필 이미지 업로드용 Presigned URL 생성")
    @PostMapping("/profile-image/presigned-url")
    fun generateProfileImagePresignedUrl(
        @Valid @RequestBody request: PresignedUrlRequest,
    ): PresignedUrlResponse = teamService.generateProfileImagePresignedUrl(request)

    @AllowIncompleteSetup
    @Operation(summary = "팀 상세 조회")
    @GetMapping("/{teamId}")
    fun getTeam(
        @PathVariable teamId: Long,
        @CurrentUser user: User?,
    ): TeamResponse = teamService.getTeam(teamId, user)

    @Operation(summary = "팀 수정")
    @PutMapping("/{teamId}")
    fun updateTeam(
        @PathVariable teamId: Long,
        @Valid @RequestBody request: TeamUpsertRequest,
        @CurrentUser user: User,
    ): TeamResponse = teamService.updateTeam(teamId, request, user)

    @Operation(summary = "팀 상태 변경")
    @PatchMapping("/{teamId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateTeamStatus(
        @PathVariable teamId: Long,
        @Valid @RequestBody request: TeamStatusUpdateRequest,
        @CurrentUser user: User,
    ) = teamService.updateTeamStatus(teamId, request, user)

    @Operation(summary = "팀 삭제")
    @DeleteMapping("/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTeam(
        @PathVariable teamId: Long,
        @CurrentUser user: User,
    ) {
        teamService.deleteTeam(teamId, user)
    }
}
