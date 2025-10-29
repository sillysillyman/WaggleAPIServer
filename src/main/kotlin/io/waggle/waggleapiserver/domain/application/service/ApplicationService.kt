package io.waggle.waggleapiserver.domain.application.service

import io.waggle.waggleapiserver.domain.application.Application
import io.waggle.waggleapiserver.domain.application.dto.response.ApplicationResponse
import io.waggle.waggleapiserver.domain.application.repository.ApplicationRepository
import io.waggle.waggleapiserver.domain.recruitment.repository.RecruitmentRepository
import io.waggle.waggleapiserver.domain.user.User
import jakarta.persistence.EntityNotFoundException
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ApplicationService(
    private val applicationRepository: ApplicationRepository,
    private val recruitmentRepository: RecruitmentRepository,
) {
    @Transactional
    fun createApplication(
        projectId: Long,
        user: User,
    ) {
        val position = user.position ?: throw IllegalStateException("User must have position")
        val recruitment =
            recruitmentRepository.findByProjectIdAndPosition(projectId, position)
                ?: throw EntityNotFoundException("Recruitment not found: $projectId, $position")

        if (!recruitment.isRecruiting()) {
            throw IllegalStateException("$position is no longer recruiting")
        }

        if (applicationRepository.existsByProjectIdAndUserIdAndPosition(
                projectId,
                user.id,
                position,
            )
        ) {
            throw DuplicateKeyException("Already applied to project: $projectId")
        }

        val application =
            Application(
                position = position,
                projectId = projectId,
                userId = user.id,
            )

        applicationRepository.save(application)
    }

    fun getUserApplications(userId: UUID): List<ApplicationResponse> {
        val applications = applicationRepository.findByUserId(userId)
        return applications.map { ApplicationResponse.from(it) }
    }
}
