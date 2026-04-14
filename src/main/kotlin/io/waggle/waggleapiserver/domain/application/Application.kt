package io.waggle.waggleapiserver.domain.application

import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.user.enums.Position
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "applications",
    uniqueConstraints = [UniqueConstraint(columnNames = ["team_id", "user_id", "position"])],
    indexes = [
        Index(name = "idx_applications_user", columnList = "user_id"),
        Index(name = "idx_applications_team_status", columnList = "team_id, status"),
        Index(name = "idx_applications_post", columnList = "post_id"),
        Index(name = "idx_applications_team_priority_id", columnList = "team_id, status_priority, id DESC"),
        Index(name = "idx_applications_post_priority_id", columnList = "post_id, status_priority, id DESC"),
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
    @Column(name = "team_id", nullable = false, updatable = false)
    val teamId: Long,
    @Column(name = "post_id", nullable = false, updatable = false)
    val postId: Long,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(columnDefinition = "VARCHAR(5000)")
    var detail: String?,
) : AuditingEntity() {
    @Column(name = "status_priority", insertable = false, updatable = false, columnDefinition = "TINYINT")
    val statusPriority: Int = 0

    @ElementCollection
    @CollectionTable(name = "application_portfolio_urls", joinColumns = [JoinColumn(name = "application_id")])
    @Column(name = "portfolio_url", nullable = false, columnDefinition = "VARCHAR(2048)")
    val portfolioUrls: MutableList<String> = mutableListOf()

    fun updateStatus(status: ApplicationStatus) {
        if (this.status != ApplicationStatus.PENDING) {
            throw BusinessException(ErrorCode.INVALID_STATE, "status is not PENDING")
        }
        this.status = status
    }
}
