package io.waggle.waggleapiserver.domain.recruitment.repository

import io.waggle.waggleapiserver.domain.recruitment.Recruitment
import io.waggle.waggleapiserver.domain.user.enums.Position
import org.springframework.data.jpa.repository.JpaRepository

interface RecruitmentRepository : JpaRepository<Recruitment, Long> {
    fun findByProjectIdAndPosition(
        projectId: Long,
        position: Position,
    ): Recruitment?

    fun findAllByProjectId(projectId: Long): List<Recruitment>
}
