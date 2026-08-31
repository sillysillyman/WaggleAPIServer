package io.waggle.waggleapiserver.domain.post

import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.Bookmarkable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.util.UUID

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
    name = "posts",
    indexes = [
        Index(name = "idx_posts_title", columnList = "title"),
        Index(name = "idx_posts_team", columnList = "team_id"),
        Index(name = "idx_posts_user", columnList = "user_id"),
    ],
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
    @Column(name = "team_id", nullable = false, updatable = false)
    val teamId: Long,
) : AuditingEntity(),
    Bookmarkable {
    // 스케줄러의 native UPDATE만 이 컬럼을 씀
    @Column(name = "view_count", nullable = false, insertable = false, updatable = false)
    val viewCount: Long = 0

    override val targetId: Long
        get() = id
    override val type: BookmarkType
        get() = BookmarkType.POST

    fun update(
        title: String,
        content: String,
    ) {
        this.title = title
        this.content = content
    }

    fun checkOwnership(currentUserId: UUID) {
        if (userId != currentUserId) {
            throw BusinessException(ErrorCode.ACCESS_DENIED, "Not the owner of the post")
        }
    }
}
