package io.waggle.waggleapiserver.domain.project.service

import io.waggle.waggleapiserver.domain.member.Member
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.project.Project
import io.waggle.waggleapiserver.domain.project.dto.request.ProjectUpsertRequest
import io.waggle.waggleapiserver.domain.project.repository.ProjectRepository
import io.waggle.waggleapiserver.domain.user.User
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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

        val (name, description) = request
        val project =
            projectRepository.findByIdOrNull(projectId)
                ?: throw EntityNotFoundException("Project not found: $projectId")

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

        projectRepository.deleteById(projectId)
    }
}
