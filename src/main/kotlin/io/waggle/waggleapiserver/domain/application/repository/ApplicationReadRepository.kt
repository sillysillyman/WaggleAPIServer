package io.waggle.waggleapiserver.domain.application.repository

import io.waggle.waggleapiserver.domain.application.ApplicationRead
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ApplicationReadRepository : JpaRepository<ApplicationRead, Long> {
    fun existsByApplicationIdAndUserId(
        applicationId: Long,
        userId: UUID,
    ): Boolean

    @Query(
        """
        SELECT ar.applicationId FROM ApplicationRead ar
        WHERE ar.userId = :userId
        AND ar.applicationId IN :applicationIds
        """,
    )
    fun findReadApplicationIds(
        userId: UUID,
        applicationIds: List<Long>,
    ): List<Long>

    fun findByApplicationIdInAndUserIdIn(
        applicationIds: List<Long>,
        userIds: List<UUID>,
    ): List<ApplicationRead>
}
