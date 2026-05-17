package io.waggle.waggleapiserver.domain.term.repository

import io.waggle.waggleapiserver.domain.term.UserTermAgreement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserTermAgreementRepository : JpaRepository<UserTermAgreement, Long> {
    fun findByUserIdAndTermId(
        userId: UUID,
        termId: Long,
    ): UserTermAgreement?

    @Query(
        """
        SELECT COUNT(uta) FROM UserTermAgreement uta
        WHERE uta.userId = :userId
        AND uta.termId IN :termIds
        AND uta.withdrawnAt IS NULL
        """,
    )
    fun countActiveByUserIdAndTermIdIn(
        userId: UUID,
        termIds: List<Long>,
    ): Long

    @Query(
        """
        SELECT uta.termId FROM UserTermAgreement uta
        WHERE uta.userId = :userId AND uta.withdrawnAt IS NULL
        """,
    )
    fun findActiveTermIdsByUserId(userId: UUID): List<Long>

    fun deleteByUserId(userId: UUID)
}
