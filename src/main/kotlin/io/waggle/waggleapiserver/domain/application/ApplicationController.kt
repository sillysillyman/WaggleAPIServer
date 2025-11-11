package io.waggle.waggleapiserver.domain.application

import io.waggle.waggleapiserver.common.util.CurrentUser
import io.waggle.waggleapiserver.domain.application.dto.response.ApplicationResponse
import io.waggle.waggleapiserver.domain.application.service.ApplicationService
import io.waggle.waggleapiserver.domain.user.User
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/applications")
@RestController
class ApplicationController(
    private val applicationService: ApplicationService,
) {
    @PatchMapping("/{applicationId}/approve")
    fun approveApplication(
        @PathVariable applicationId: Long,
        @CurrentUser user: User,
    ): ApplicationResponse = applicationService.approveApplication(applicationId, user)

    @PatchMapping("/{applicationId}/reject")
    fun rejectApplication(
        @PathVariable applicationId: Long,
        @CurrentUser user: User,
    ): ApplicationResponse = applicationService.rejectApplication(applicationId, user)

    @DeleteMapping("/{applicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteApplication(
        @PathVariable applicationId: Long,
        @CurrentUser user: User,
    ) {
        applicationService.deleteApplication(applicationId, user)
    }
}
