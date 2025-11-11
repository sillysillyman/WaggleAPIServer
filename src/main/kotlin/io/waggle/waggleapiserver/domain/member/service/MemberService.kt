package io.waggle.waggleapiserver.domain.member.service

import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.user.User
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
) {
    @Transactional
    fun leaveProject(
        projectId: Long,
        user: User,
    ) {
        val member =
            memberRepository.findByUserIdAndProjectId(user.id, projectId)
                ?: throw EntityNotFoundException("Member Not Found")

        if (member.isLeader) {
            throw IllegalArgumentException("Leader cannot leave project")
        }

        member.delete()
    }
}
