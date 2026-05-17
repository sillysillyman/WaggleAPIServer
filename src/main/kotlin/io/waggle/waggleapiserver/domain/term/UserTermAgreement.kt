package io.waggle.waggleapiserver.domain.term

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "user_term_agreements",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_term_agreements_user_term",
            columnNames = ["user_id", "term_id"],
        ),
    ],
)
class UserTermAgreement(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "term_id", nullable = false, updatable = false)
    val termId: Long,
    @Column(name = "agreed_at", nullable = false, updatable = false)
    val agreedAt: Instant = Instant.now(),
    @Column(name = "withdrawn_at")
    var withdrawnAt: Instant? = null,
)
