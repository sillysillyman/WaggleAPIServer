package io.waggle.waggleapiserver.domain.project

import io.waggle.waggleapiserver.common.util.CurrentUser
import io.waggle.waggleapiserver.domain.project.dto.request.ProjectUpsertRequest
import io.waggle.waggleapiserver.domain.project.dto.response.ProjectSimpleResponse
import io.waggle.waggleapiserver.domain.project.service.ProjectService
import io.waggle.waggleapiserver.domain.user.User
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/projects")
@RestController
class ProjectController(
    private val projectService: ProjectService,
) {
    @PostMapping
    fun createProject(
        @Valid @RequestBody request: ProjectUpsertRequest,
        @CurrentUser user: User,
    ) {
        projectService.createProject(request, user)
    }

    @GetMapping("/{projectId}")
    fun getProject(
        @PathVariable("projectId") projectId: Long,
    ): ResponseEntity<ProjectSimpleResponse> = ResponseEntity.ok(projectService.getProject(projectId))

    @PutMapping("/{projectId}")
    fun updateProject(
        @PathVariable projectId: Long,
        @Valid @RequestBody request: ProjectUpsertRequest,
        @CurrentUser user: User,
    ) {
        projectService.updateProject(projectId, request, user)
    }

    @DeleteMapping("/{projectId}")
    fun deleteProject(
        @PathVariable projectId: Long,
        @CurrentUser user: User,
    ) {
        projectService.deleteProject(projectId, user)
    }
}
