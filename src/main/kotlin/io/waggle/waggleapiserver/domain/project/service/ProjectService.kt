package io.waggle.waggleapiserver.domain.project.service

import io.waggle.waggleapiserver.domain.member.Member
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.dto.response.MemberResponse
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.project.Project
import io.waggle.waggleapiserver.domain.project.dto.request.ProjectUpsertRequest
import io.waggle.waggleapiserver.domain.project.dto.response.ProjectDetailResponse
import io.waggle.waggleapiserver.domain.project.repository.ProjectRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProjectService(
    private val memberRepository: MemberRepository,
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun createProject(
        request: ProjectUpsertRequest,
        user: User,
    ): ProjectDetailResponse {
        val (name, description, thumbnailUrl) = request

        if (projectRepository.existsByName(name)) {
            throw DuplicateKeyException("Already exists project name: $name")
        }

        val project =
            Project(
                name = name,
                description = description,
                thumbnailUrl = thumbnailUrl,
                leaderId = user.id,
                creatorId = user.id,
            )

        val savedProject = projectRepository.save(project)

        val member =
            Member(
                userId = user.id,
                projectId = savedProject.id,
                role = MemberRole.LEADER,
            )

        val savedMember = memberRepository.save(member)

        return ProjectDetailResponse.of(savedProject, listOf(MemberResponse.of(savedMember, user)))
    }

    fun getProject(projectId: Long): ProjectDetailResponse {
        val project =
            projectRepository.findByIdOrNull(projectId)
                ?: throw EntityNotFoundException("Project not found: $projectId")
        val members = memberRepository.findByProjectIdOrderByCreatedAtAsc(projectId)
        val users = userRepository.findAllById(members.map { it.userId }).associateBy { it.id }

        return ProjectDetailResponse.of(
            project,
            members.map {
                MemberResponse.of(
                    it,
                    users[it.userId]!!,
                )
            },
        )
    }

    @Transactional
    fun updateProject(
        projectId: Long,
        request: ProjectUpsertRequest,
        user: User,
    ): ProjectDetailResponse {
        val member =
            memberRepository.findByUserIdAndProjectId(user.id, projectId)
                ?: throw EntityNotFoundException("Member not found: ${user.id}, $projectId")
        member.checkMemberRole(MemberRole.MANAGER)

        val project =
            projectRepository.findByIdOrNull(projectId)
                ?: throw EntityNotFoundException("Project not found: $projectId")

        val (name, description, thumbnailUrl) = request

        if (name != project.name && projectRepository.existsByName(name)) {
            throw DuplicateKeyException("Already exists project name: $name")
        }

        project.update(
            name = name,
            description = description,
            thumbnailUrl = thumbnailUrl,
        )

        val members = memberRepository.findByProjectIdOrderByCreatedAtAsc(projectId)
        val users = userRepository.findAllById(members.map { it.userId }).associateBy { it.id }

        return ProjectDetailResponse.of(
            project,
            members.map {
                MemberResponse.of(
                    it,
                    users[it.userId]!!,
                )
            },
        )
    }

    @Transactional
    fun deleteProject(
        projectId: Long,
        user: User,
    ) {
        val member =
            memberRepository.findByUserIdAndProjectId(user.id, projectId)
                ?: throw EntityNotFoundException("Member not found: ${user.id}, $projectId")
        member.checkMemberRole(MemberRole.LEADER)

        val project =
            projectRepository.findByIdOrNull(projectId)
                ?: throw EntityNotFoundException("Project not found: $projectId")

        project.delete()
    }
}
