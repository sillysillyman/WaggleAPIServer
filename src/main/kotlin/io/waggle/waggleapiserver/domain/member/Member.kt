package io.waggle.waggleapiserver.domain.member

import io.waggle.waggleapiserver.common.AuditingEntity
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
import org.springframework.security.access.AccessDeniedException
import java.util.UUID

@Entity
@Table(
    name = "members",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "project_id"])],
    indexes = [Index(name = "idx_members_project_id", columnList = "project_id")],
)
class Member(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "project_id", nullable = false, updatable = false)
    val projectId: Long,
    @Enumerated(EnumType.STRING)
    var role: MemberRole = MemberRole.MEMBER,
) : AuditingEntity() {
    fun checkMembership(requiredRole: MemberRole) {
        if (role.level < requiredRole.level) {
            throw AccessDeniedException("$role do not have the authority")
        }
    }
}
