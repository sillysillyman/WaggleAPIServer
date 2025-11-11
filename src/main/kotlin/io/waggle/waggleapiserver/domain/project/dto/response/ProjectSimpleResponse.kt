package io.waggle.waggleapiserver.domain.project.dto.response

import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkResponse
import io.waggle.waggleapiserver.domain.project.Project
import java.time.Instant

data class ProjectSimpleResponse(
    val projectId: Long,
    val name: String,
    val description: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) : BookmarkResponse {
    companion object {
        fun from(project: Project) =
            ProjectSimpleResponse(
                projectId = project.id,
                name = project.name,
                description = project.description,
                createdAt = project.createdAt,
                updatedAt = project.updatedAt,
            )
    }
}
