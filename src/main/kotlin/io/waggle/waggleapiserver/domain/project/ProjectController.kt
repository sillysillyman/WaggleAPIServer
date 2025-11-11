package io.waggle.waggleapiserver.domain.project

import io.waggle.waggleapiserver.common.util.CurrentUser
import io.waggle.waggleapiserver.domain.application.dto.response.ApplicationResponse
import io.waggle.waggleapiserver.domain.application.service.ApplicationService
import io.waggle.waggleapiserver.domain.project.dto.request.ProjectUpsertRequest
import io.waggle.waggleapiserver.domain.project.dto.response.ProjectDetailResponse
import io.waggle.waggleapiserver.domain.project.service.ProjectService
import io.waggle.waggleapiserver.domain.recruitment.dto.request.RecruitmentUpsertRequest
import io.waggle.waggleapiserver.domain.recruitment.dto.response.RecruitmentResponse
import io.waggle.waggleapiserver.domain.recruitment.service.RecruitmentService
import io.waggle.waggleapiserver.domain.user.User
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/projects")
@RestController
class ProjectController(
    private val applicationService: ApplicationService,
    private val recruitmentService: RecruitmentService,
    private val projectService: ProjectService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createProject(
        @Valid @RequestBody request: ProjectUpsertRequest,
        @CurrentUser user: User,
    ): ProjectDetailResponse = projectService.createProject(request, user)

    @PostMapping("/{projectId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    fun applyProject(
        @PathVariable projectId: Long,
        @CurrentUser user: User,
    ): ApplicationResponse = applicationService.applyProject(projectId, user)

    @PostMapping("/{projectId}/recruitments")
    @ResponseStatus(HttpStatus.CREATED)
    fun createProjectRecruitments(
        @PathVariable projectId: Long,
        @Valid @RequestBody request: List<RecruitmentUpsertRequest>,
        @CurrentUser user: User,
    ): List<RecruitmentResponse> = recruitmentService.createRecruitments(projectId, request, user)

    @GetMapping("/{projectId}")
    fun getProject(
        @PathVariable projectId: Long,
    ): ProjectDetailResponse = projectService.getProject(projectId)

    @GetMapping("/{projectId}/applications")
    fun getProjectApplications(
        @PathVariable projectId: Long,
        @CurrentUser user: User,
    ): List<ApplicationResponse> = applicationService.getProjectApplications(projectId, user)

    @PutMapping("/{projectId}")
    fun updateProject(
        @PathVariable projectId: Long,
        @Valid @RequestBody request: ProjectUpsertRequest,
        @CurrentUser user: User,
    ): ProjectDetailResponse = projectService.updateProject(projectId, request, user)

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteProject(
        @PathVariable projectId: Long,
        @CurrentUser user: User,
    ) {
        projectService.deleteProject(projectId, user)
    }
}
