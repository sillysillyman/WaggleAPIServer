package io.waggle.waggleapiserver.domain.post

import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.Bookmarkable
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

@Access(AccessType.FIELD)
@Entity
@Table(
    name = "posts",
    indexes = [Index(name = "idx_posts_title", columnList = "title")],
)
class Post(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    var title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "team_id", nullable = false)
    var teamId: Long,
) : AuditingEntity(),
    Bookmarkable {
    override val targetId: Long
        get() = id
    override val type: BookmarkType
        get() = BookmarkType.POST

    fun update(
        title: String,
        content: String,
        teamId: Long,
    ) {
        this.title = title
        this.content = content
        this.teamId = teamId
    }

    fun checkOwnership(currentUserId: UUID) {
        if (userId != currentUserId) {
            throw BusinessException(ErrorCode.ACCESS_DENIED, "Not the owner of the post")
        }
    }
}
