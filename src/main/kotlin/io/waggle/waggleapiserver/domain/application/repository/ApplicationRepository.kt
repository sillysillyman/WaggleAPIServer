package io.waggle.waggleapiserver.domain.application.repository

import io.waggle.waggleapiserver.domain.application.Application
import io.waggle.waggleapiserver.domain.user.enums.Position
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ApplicationRepository : JpaRepository<Application, Long> {
    fun existsByProjectIdAndUserIdAndPosition(
        projectId: Long,
        userId: UUID,
        position: Position,
    ): Boolean

    fun findByUserId(userId: UUID): List<Application>
}
