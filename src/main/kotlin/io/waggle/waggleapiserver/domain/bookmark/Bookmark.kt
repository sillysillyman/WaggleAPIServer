package io.waggle.waggleapiserver.domain.bookmark

import io.waggle.waggleapiserver.domain.user.User
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Embeddable
data class BookmarkId(
    @Column(nullable = false, updatable = false)
    val userId: UUID,
    @Column(nullable = false, updatable = false)
    val bookmarkableId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    val bookmarkType: BookmarkType,
) : Serializable

@Entity
@Table(
    name = "bookmarks",
    indexes = [Index(name = "idx_bookmarkable", columnList = "bookmark_type, bookmarkable_id")],
)
class Bookmark(
    @EmbeddedId
    val id: BookmarkId,
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", updatable = false, insertable = false)
    val user: User,
    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) {
    val bookmarkableId: Long get() = id.bookmarkableId
    val bookmarkType: BookmarkType get() = id.bookmarkType
}

enum class BookmarkType {
    POST,
    PROJECT,
}
