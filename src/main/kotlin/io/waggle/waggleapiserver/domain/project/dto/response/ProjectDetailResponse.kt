package io.waggle.waggleapiserver.domain.project.dto.response

import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkResponse
import io.waggle.waggleapiserver.domain.member.dto.response.MemberResponse
import io.waggle.waggleapiserver.domain.project.Project
import java.time.Instant

data class ProjectDetailResponse(
    val projectId: Long,
    val name: String,
    val description: String,
    val members: List<MemberResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
) : BookmarkResponse {
    companion object {
        fun of(
            project: Project,
            members: List<MemberResponse>,
        ) = ProjectDetailResponse(
            projectId = project.id,
            name = project.name,
            description = project.description,
            members = members,
            createdAt = project.createdAt,
            updatedAt = project.updatedAt,
        )
    }
}
