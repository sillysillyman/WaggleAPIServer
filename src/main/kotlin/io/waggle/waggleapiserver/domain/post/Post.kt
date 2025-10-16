package io.waggle.waggleapiserver.domain.post

import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.Bookmarkable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.springframework.security.access.AccessDeniedException
import java.util.UUID

@Entity
@Table(
    name = "posts",
    indexes = [Index(name = "idx_title", columnList = "title")],
)
class Post(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    var title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,
    @Column(name = "user_id", nullable = false)
    val userId: UUID,
    @Column(name = "project_id")
    var projectId: Long?,
) : AuditingEntity(),
    Bookmarkable {
    override val bookmarkableId: Long get() = id
    override val bookmarkType: BookmarkType = BookmarkType.POST

    fun update(
        title: String,
        content: String,
        projectId: Long?,
    ) {
        this.title = title
        this.content = content
        this.projectId = projectId
    }

    fun checkOwnership(currentUserId: UUID) {
        if (userId != currentUserId) {
            throw AccessDeniedException("Not the owner of the post")
        }
    }
}
