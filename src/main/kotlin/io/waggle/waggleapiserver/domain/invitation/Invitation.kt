package io.waggle.waggleapiserver.domain.invitation

import io.waggle.waggleapiserver.common.AuditingEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "invitations")
class Invitation(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false)
    var userId: Long,
    @Column(name = "project_id", nullable = false)
    var projectId: Long,
    @Column(name = "application_id")
    var applicationId: Long? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20))")
    var status: InvitationStatus = InvitationStatus.PENDING,
    @Column(name = "expires_at")
    var expiresAt: Instant? = null,
    @Column(name = "accepted_at")
    var acceptedAt: Instant? = null,
    @Column(name = "declined_at")
    var declinedAt: Instant? = null,
) : AuditingEntity() {
    val isExpired: Boolean
        get() =
            expiresAt?.let {
                Instant.now().isAfter(it)
            } ?: false

    val isFromApplication: Boolean
        get() = applicationId != null
}
