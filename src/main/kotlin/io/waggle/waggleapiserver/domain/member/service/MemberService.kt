package io.waggle.waggleapiserver.domain.member.service

import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
    private val userRepository: UserRepository,
) {
    fun getProjectMembers(projectId: Long): List<UserSimpleResponse> {
        val userIds = memberRepository.findByProjectId(projectId).map { it.user.id }
        val users = userRepository.findByIdIn(userIds)
        return users.map { UserSimpleResponse.from(it) }
    }
}
