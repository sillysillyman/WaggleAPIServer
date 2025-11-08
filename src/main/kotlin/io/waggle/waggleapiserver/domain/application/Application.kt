package io.waggle.waggleapiserver.domain.application

import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.domain.user.enums.Position
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "applications",
    uniqueConstraints = [UniqueConstraint(columnNames = ["project_id", "user_id", "position"])],
    indexes = [
        Index(name = "idx_applications_project_id", columnList = "project_id"),
        Index(name = "idx_applications_user_id", columnList = "user_id"),
        Index(name = "idx_applications_project_id_status", columnList = "project_id, status"),
    ],
)
class Application(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    val position: Position,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    var status: ApplicationStatus = ApplicationStatus.PENDING,
    @Column(name = "project_id", nullable = false, updatable = false)
    val projectId: Long,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
) : AuditingEntity()
