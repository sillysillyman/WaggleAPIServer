package io.waggle.waggleapiserver.domain.recruitment

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

@Entity
@Table(
    name = "recruitments",
    uniqueConstraints = [UniqueConstraint(columnNames = ["project_id", "position"])],
    indexes = [Index(name = "idx_recruitments_project_id", columnList = "project_id")],
)
class Recruitment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    val position: Position,
    @Column(name = "current_count", nullable = false)
    var currentCount: Int = 0,
    @Column(name = "recruiting_count", nullable = false)
    val recruitingCount: Int,
    @Column(name = "project_id", nullable = false)
    val projectId: Long,
) : AuditingEntity() {
    fun isRecruiting(): Boolean = currentCount < recruitingCount
}
