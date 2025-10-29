package io.waggle.waggleapiserver.domain.bookmark

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Embeddable
data class BookmarkId(
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "bookmarkable_id", nullable = false, updatable = false)
    val bookmarkableId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "bookmark_type", nullable = false, updatable = false)
    val bookmarkType: BookmarkType,
) : Serializable

@Entity
@Table(
    name = "bookmarks",
    indexes = [
        Index(
            name = "idx_bookmarks_bookmarkable",
            columnList = "bookmark_type, bookmarkable_id",
        ),
    ],
)
class Bookmark(
    @EmbeddedId
    val id: BookmarkId,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) {
    val bookmarkableId: Long get() = id.bookmarkableId
    val bookmarkType: BookmarkType get() = id.bookmarkType
}

enum class BookmarkType {
    POST,
    PROJECT,
}
