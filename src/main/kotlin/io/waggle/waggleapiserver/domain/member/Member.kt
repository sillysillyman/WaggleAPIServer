package io.waggle.waggleapiserver.domain.member

import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.domain.member.MemberRole.Permission
import io.waggle.waggleapiserver.domain.project.Project
import io.waggle.waggleapiserver.domain.user.User
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.security.access.AccessDeniedException

@Entity
@Table(
    name = "members",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "project_id"])],
    indexes = [Index(name = "idx_project_id", columnList = "project_id")],
)
class Member(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "project_id", nullable = false)
    val project: Project,
    @Enumerated(EnumType.STRING)
    var role: MemberRole = MemberRole.MEMBER,
) : AuditingEntity() {
    fun checkPostCreation() {
        if (!role.hasPermission(Permission.CREATE_POST)) {
            throw AccessDeniedException("$role cannot create posts")
        }
    }

    fun checkPostDeletion() {
        if (!role.hasPermission(Permission.DELETE_POST)) {
            throw AccessDeniedException("$role cannot delete posts")
        }
    }

    fun checkProjectUpdate() {
        if (!role.hasPermission(Permission.MODIFY_PROJECT)) {
            throw AccessDeniedException("$role cannot update project")
        }
    }

    fun checkProjectDeletion() {
        if (!role.hasPermission(Permission.DELETE_PROJECT)) {
            throw AccessDeniedException("Only LEADER can delete project")
        }
    }

    fun checkMemberManagement() {
        if (!role.hasPermission(Permission.MANAGE_MEMBERS)) {
            throw AccessDeniedException("$role cannot manage members")
        }
    }
}
