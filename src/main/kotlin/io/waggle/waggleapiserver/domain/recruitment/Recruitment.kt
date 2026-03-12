package io.waggle.waggleapiserver.domain.recruitment

import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.enums.Skill
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "recruitments",
    uniqueConstraints = [UniqueConstraint(columnNames = ["post_id", "position"])],
    indexes = [Index(name = "idx_recruitments_post", columnList = "post_id")],
)
class Recruitment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    val position: Position,
    @Column(nullable = false)
    val count: Int,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    var status: RecruitmentStatus = RecruitmentStatus.RECRUITING,
    @Column(name = "post_id", nullable = false, updatable = false)
    val postId: Long,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recruitment_skills", joinColumns = [JoinColumn(name = "recruitment_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "skill", nullable = false, columnDefinition = "VARCHAR(30)")
    val skills: MutableSet<Skill> = mutableSetOf(),
) : AuditingEntity() {
    fun isRecruiting(): Boolean = status == RecruitmentStatus.RECRUITING

    fun close() {
        if (status == RecruitmentStatus.CLOSED) {
            throw BusinessException(ErrorCode.INVALID_STATE, "Recruitment is already closed")
        }
        status = RecruitmentStatus.CLOSED
    }
}
