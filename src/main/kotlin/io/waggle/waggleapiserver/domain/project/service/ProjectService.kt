package io.waggle.waggleapiserver.domain.project.service

import io.waggle.waggleapiserver.domain.member.Member
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.project.Project
import io.waggle.waggleapiserver.domain.project.dto.request.ProjectUpsertRequest
import io.waggle.waggleapiserver.domain.project.dto.response.ProjectSimpleResponse
import io.waggle.waggleapiserver.domain.project.repository.ProjectRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import jakarta.persistence.EntityNotFoundException
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ProjectService(
    private val memberRepository: MemberRepository,
    private val projectRepository: ProjectRepository,
) {
    @Transactional
    fun createProject(
        request: ProjectUpsertRequest,
        user: User,
    ) {
        val (name, description) = request

        if (projectRepository.existsByName(name)) {
            throw DuplicateKeyException("Already exists project name: $name")
        }

        val project =
            Project(
                name = name,
                description = description,
                user = user,
            )
        val member =
            Member(
                user = user,
                project = project,
                role = MemberRole.LEADER,
            )

        projectRepository.save(project)
        memberRepository.save(member)
    }

    fun getProject(projectId: Long): ProjectSimpleResponse {
        val project =
            projectRepository.findByIdOrNull(projectId)
                ?: throw EntityNotFoundException("Project not found: $projectId")
        return ProjectSimpleResponse.from(project)
    }

    fun getUserProjects(userId: UUID): List<ProjectSimpleResponse> =
        memberRepository
            .findAllByUserIdWithProjectOrderByCreatedAtAsc(userId)
            .map { ProjectSimpleResponse.from(it.project) }

    @Transactional
    fun updateProject(
        projectId: Long,
        request: ProjectUpsertRequest,
        user: User,
    ) {
        val member =
            memberRepository.findByUserIdAndProjectId(user.id, projectId)
                ?: throw EntityNotFoundException("Member not found: $user.id, $projectId")
        member.checkProjectUpdate()

        val project =
            projectRepository.findByIdOrNull(projectId)
                ?: throw EntityNotFoundException("Project not found: $projectId")

        val (name, description) = request

        if (name != project.name && projectRepository.existsByName(name)) {
            throw DuplicateKeyException("Already exists project name: $name")
        }

        project.update(
            name = name,
            description = description,
        )
    }

    @Transactional
    fun deleteProject(
        projectId: Long,
        user: User,
    ) {
        val member =
            memberRepository.findByUserIdAndProjectId(user.id, projectId)
                ?: throw EntityNotFoundException("Member not found: $user.id, $projectId")
        member.checkProjectDeletion()

        val project =
            projectRepository.findByIdOrNull(projectId)
                ?: throw EntityNotFoundException("Project not found: $projectId")

        project.delete()
    }
}
