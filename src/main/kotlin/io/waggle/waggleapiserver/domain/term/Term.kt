package io.waggle.waggleapiserver.domain.term

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(
    name = "terms",
    uniqueConstraints = [UniqueConstraint(name = "uk_terms_type_version", columnNames = ["type", "version"])],
)
class Term(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(40)")
    val type: TermType,
    @Column(nullable = false)
    val version: Int,
    @Column(name = "content_url", nullable = false, columnDefinition = "VARCHAR(2048)")
    var contentUrl: String,
    @Column(nullable = false)
    val mandatory: Boolean,
    @Column(name = "activated_at", nullable = false)
    val activatedAt: Instant,
    @Column(name = "deprecated_at")
    var deprecatedAt: Instant? = null,
) {
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: Instant
}
