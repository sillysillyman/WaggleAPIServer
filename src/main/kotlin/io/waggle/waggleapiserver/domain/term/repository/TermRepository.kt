package io.waggle.waggleapiserver.domain.term.repository

import io.waggle.waggleapiserver.domain.term.Term
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TermRepository : JpaRepository<Term, Long> {
    @Query(
        """
        SELECT t FROM Term t
        WHERE t.deprecatedAt IS NULL
        AND t.version = (
            SELECT MAX(t2.version) FROM Term t2
            WHERE t2.type = t.type
            AND t2.deprecatedAt IS NULL
        )
        """,
    )
    fun findLatestActive(): List<Term>
}
