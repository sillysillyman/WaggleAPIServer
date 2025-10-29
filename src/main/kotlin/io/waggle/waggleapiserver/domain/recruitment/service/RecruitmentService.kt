package io.waggle.waggleapiserver.domain.recruitment.service

import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.recruitment.Recruitment
import io.waggle.waggleapiserver.domain.recruitment.dto.request.RecruitmentUpsertRequest
import io.waggle.waggleapiserver.domain.recruitment.repository.RecruitmentRepository
import io.waggle.waggleapiserver.domain.user.User
import jakarta.persistence.EntityNotFoundException
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class RecruitmentService(
    private val memberRepository: MemberRepository,
    private val recruitmentRepository: RecruitmentRepository,
) {
    @Transactional
    fun createRecruitments(
        projectId: Long,
        request: List<RecruitmentUpsertRequest>,
        user: User,
    ) {
        val member =
            memberRepository.findByUserIdAndProjectId(user.id, projectId)
                ?: throw EntityNotFoundException("Member not found: ${user.id}, $projectId")
        member.checkMembership(MemberRole.MANAGER)

        val existingPositions =
            recruitmentRepository
                .findAllByProjectId(projectId)
                .map { it.position }
        val duplicates = request.map { it.position }.intersect(existingPositions.toSet())
        if (duplicates.isNotEmpty()) {
            throw DuplicateKeyException("Already exists recruitment for positions: $duplicates")
        }

        val recruitments =
            request.map {
                Recruitment(
                    position = it.position,
                    recruitingCount = it.recruitingCount,
                    projectId = projectId,
                )
            }
        recruitmentRepository.saveAll(recruitments)
    }
}
