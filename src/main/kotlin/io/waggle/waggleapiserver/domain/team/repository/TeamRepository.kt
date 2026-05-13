package io.waggle.waggleapiserver.domain.team.repository

import io.waggle.waggleapiserver.domain.team.Team
import org.springframework.data.jpa.repository.JpaRepository

interface TeamRepository : JpaRepository<Team, Long> {
    fun findByIdInOrderByCreatedAtDesc(ids: List<Long>): List<Team>
}
