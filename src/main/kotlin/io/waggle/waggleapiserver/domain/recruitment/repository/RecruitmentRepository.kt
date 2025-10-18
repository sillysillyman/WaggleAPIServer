package io.waggle.waggleapiserver.domain.recruitment.repository

import io.waggle.waggleapiserver.domain.recruitment.Recruitment
import org.springframework.data.jpa.repository.JpaRepository

interface RecruitmentRepository : JpaRepository<Recruitment, Long> {
    fun findAllByProjectId(projectId: Long): List<Recruitment>
}
