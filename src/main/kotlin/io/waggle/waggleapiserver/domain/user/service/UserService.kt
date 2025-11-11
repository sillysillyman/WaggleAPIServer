package io.waggle.waggleapiserver.domain.user.service

import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.project.dto.response.ProjectSimpleResponse
import io.waggle.waggleapiserver.domain.project.repository.ProjectRepository
import io.waggle.waggleapiserver.domain.user.dto.request.UserUpdateRequest
import io.waggle.waggleapiserver.domain.user.dto.response.UserDetailResponse
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserService(
    private val memberRepository: MemberRepository,
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
) {
    fun getUser(userId: UUID): UserDetailResponse {
        val user =
            userRepository.findByIdOrNull(userId)
                ?: throw EntityNotFoundException("User not found: $userId")
        return UserDetailResponse.from(user)
    }

    fun getUserProjects(userId: UUID): List<ProjectSimpleResponse> {
        val projectIds =
            memberRepository
                .findAllByUserIdOrderByCreatedAtAsc(userId)
                .map { it.projectId }
        val projects = projectRepository.findAllById(projectIds)

        return projects.map { ProjectSimpleResponse.from(it) }
    }

    @Transactional
    fun updateUser(
        userId: UUID,
        request: UserUpdateRequest,
    ): UserDetailResponse {
        val (username, workTime, workWay, sido, position, yearCount, detail) = request
        val user =
            userRepository.findByIdOrNull(userId)
                ?: throw EntityNotFoundException("User not found: $userId")

        if (user.username != username && userRepository.existsByUsername(username)) {
            throw DuplicateKeyException("Username already exists")
        }

        user.update(
            username = username,
            workTime = workTime,
            workWay = workWay,
            sido = sido,
            detail = detail,
            position = position,
            yearCount = yearCount,
        )

        return UserDetailResponse.from(user)
    }
}
