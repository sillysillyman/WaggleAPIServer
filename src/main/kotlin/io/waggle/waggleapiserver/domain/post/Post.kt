package io.waggle.waggleapiserver.domain.post

import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.domain.project.Project
import io.waggle.waggleapiserver.domain.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.security.access.AccessDeniedException
import java.util.UUID

@Entity
@Table(
    name = "posts",
    indexes = [Index(name = "idx_title", columnList = "title")],
)
class Post(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    @Column(nullable = false) var title: String,
    @Column(nullable = false, columnDefinition = "TEXT") var content: String,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") val user: User,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "project_id") val project: Project?,
) : AuditingEntity() {
    fun update(
        title: String,
        content: String,
    ) {
        this.title = title
        this.content = content
    }

    fun validateOwnership(userId: UUID) {
        if (user.id != userId) {
            throw AccessDeniedException("d")
        }
    }
}
