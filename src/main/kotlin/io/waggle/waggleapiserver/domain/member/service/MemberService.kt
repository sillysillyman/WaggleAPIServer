package io.waggle.waggleapiserver.domain.member.service

import io.waggle.waggleapiserver.domain.member.Member
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.dto.request.MemberUpdateRoleRequest
import io.waggle.waggleapiserver.domain.member.dto.response.MemberResponse
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.project.repository.ProjectRepository
import io.waggle.waggleapiserver.domain.user.User
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
    private val projectRepository: ProjectRepository,
) {
    @Transactional
    fun updateMemberRole(
        memberId: Long,
        request: MemberUpdateRoleRequest,
        user: User,
    ): MemberResponse {
        val (projectId, role) = request

        val member =
            memberRepository.findByUserIdAndProjectId(user.id, projectId)
                ?: throw EntityNotFoundException("Member not found")
        require(member.id != memberId) { "Cannot update your own role" }

        val targetMember =
            memberRepository.findByIdOrNull(memberId)
                ?: throw EntityNotFoundException("Member not found: $memberId")
        require(targetMember.projectId == projectId) { "Not a member of the project" }

        when (role) {
            MemberRole.MEMBER -> member.checkMemberRole(targetMember.role)
            MemberRole.MANAGER -> member.checkMemberRole(MemberRole.LEADER)
            MemberRole.LEADER -> delegateLeader(targetMember, member)
        }

        targetMember.updateRole(role)

        return MemberResponse.of(targetMember, user)
    }

    @Transactional
    fun leaveProject(
        projectId: Long,
        user: User,
    ) {
        val member =
            memberRepository.findByUserIdAndProjectId(user.id, projectId)
                ?: throw EntityNotFoundException("Member Not Found")
        val members =
            memberRepository.findAllByIdNotAndProjectIdOrderByCreatedAtAsc(member.id, projectId)
        check(members.isNotEmpty()) { "Cannot leave as the only member" }

        if (member.isLeader) {
            delegateLeader(members[0], member)
        }

        member.delete()
    }

    @Transactional
    fun removeMember(
        memberId: Long,
        user: User,
    ) {
        val member =
            memberRepository.findByIdOrNull(memberId)
                ?: throw EntityNotFoundException("Member not found: $memberId")

        val project =
            projectRepository.findByIdOrNull(member.projectId)
                ?: throw EntityNotFoundException("Project Not Found: ${member.projectId}")

        val leader =
            memberRepository.findByUserIdAndProjectId(user.id, project.id)
                ?: throw EntityNotFoundException("Member not found")
        check(leader.isLeader) { "Only leader can remove member" }

        member.delete()
    }

    private fun delegateLeader(
        member: Member,
        leader: Member,
    ) {
        check(leader.isLeader) { "Only leader can delegate authority" }
        require(member.projectId == leader.projectId) { "Not in the same project" }

        val project =
            projectRepository.findByIdOrNull(leader.projectId)
                ?: throw EntityNotFoundException("Project Not Found: ${leader.projectId}")

        leader.updateRole(MemberRole.MANAGER)
        member.updateRole(MemberRole.LEADER)
        project.leaderId = member.userId
    }
}
