package io.waggle.waggleapiserver.domain.application

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.dto.response.CursorResponse
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.domain.application.dto.request.ApplicationUpdateRequest
import io.waggle.waggleapiserver.domain.application.dto.request.ApplicationUpdateStatusRequest
import io.waggle.waggleapiserver.domain.application.dto.response.ApplicationResponse
import io.waggle.waggleapiserver.domain.application.dto.response.TeamApplicationResponse
import io.waggle.waggleapiserver.domain.application.dto.response.UserApplicationCountsResponse
import io.waggle.waggleapiserver.domain.application.dto.response.UserApplicationResponse
import io.waggle.waggleapiserver.domain.application.service.ApplicationService
import io.waggle.waggleapiserver.domain.user.User
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "팀 지원")
@RequestMapping("/applications")
@RestController
class ApplicationController(
    private val applicationService: ApplicationService,
) {
    @Operation(
        summary = "본인 지원 목록 조회",
        description = "status 미지정 시 전체 반환, 지정 시 해당 상태로 필터링",
    )
    @GetMapping
    fun getMyApplications(
        @RequestParam(required = false) status: ApplicationStatus?,
        @ParameterObject cursorQuery: CursorGetQuery,
        @CurrentUser user: User,
    ): CursorResponse<UserApplicationResponse> = applicationService.getUserApplications(status, cursorQuery, user)

    @Operation(
        summary = "본인 지원 상태별 개수 조회",
        description = "전체/PENDING/APPROVED/REJECTED 개수를 반환",
    )
    @GetMapping("/count")
    fun getMyApplicationCounts(
        @CurrentUser user: User,
    ): UserApplicationCountsResponse = applicationService.getUserApplicationCounts(user)

    @Operation(
        summary = "팀 지원 읽음 처리",
        description = "팀 관리자가 지원을 읽음 처리함",
    )
    @PostMapping("/{applicationId}/read")
    fun markApplicationAsRead(
        @PathVariable applicationId: Long,
        @CurrentUser user: User,
    ): TeamApplicationResponse = applicationService.markApplicationAsRead(applicationId, user)

    @Operation(
        summary = "팀 지원 상태 변경",
        description = "팀 관리자가 지원 상태를 변경함 (승인/거절)",
    )
    @PatchMapping("/{applicationId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateApplicationStatus(
        @PathVariable applicationId: Long,
        @Valid @RequestBody request: ApplicationUpdateStatusRequest,
        @CurrentUser user: User,
    ) = applicationService.updateApplicationStatus(applicationId, request.status, user)

    @Operation(
        summary = "팀 지원 내용 수정",
        description = "팀 지원자가 본인의 PENDING 상태의 지원 내용을 수정함",
    )
    @PutMapping("/{applicationId}")
    fun updateApplication(
        @PathVariable applicationId: Long,
        @Valid @RequestBody request: ApplicationUpdateRequest,
        @CurrentUser user: User,
    ): ApplicationResponse = applicationService.updateApplication(applicationId, request, user)

    @Operation(
        summary = "팀 지원 취소",
        description = "팀 지원자가 본인의 검토 중인 지원 내역을 취소함",
    )
    @DeleteMapping("/{applicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteApplication(
        @PathVariable applicationId: Long,
        @CurrentUser user: User,
    ) {
        applicationService.deleteApplication(applicationId, user)
    }
}
