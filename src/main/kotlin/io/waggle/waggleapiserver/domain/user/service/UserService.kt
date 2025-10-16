package io.waggle.waggleapiserver.domain.user.service

import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.user.dto.request.UserUpdateRequest
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
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
    private val userRepository: UserRepository,
) {
    fun getProjectUsers(projectId: Long): List<UserSimpleResponse> {
        val userIds =
            memberRepository
                .findAllByProjectIdOrderByCreatedAtAsc(projectId)
                .map { it.userId }
        val users = userRepository.findAllById(userIds)

        return users.map { UserSimpleResponse.from(it) }
    }

    @Transactional
    fun updateUser(
        userId: UUID,
        request: UserUpdateRequest,
    ): UserSimpleResponse {
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

        return UserSimpleResponse.from(user)
    }
}
