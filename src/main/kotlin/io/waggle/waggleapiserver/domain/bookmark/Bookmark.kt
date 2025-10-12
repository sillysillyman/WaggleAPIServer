package io.waggle.waggleapiserver.domain.bookmark

import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.domain.user.User
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import java.io.Serializable
import java.util.UUID

@Embeddable
data class BookmarkId(
    @Column(name = "user_id")
    val userId: UUID,
    @Column(name = "bookmarkable_id")
    val bookmarkableId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "bookmarkable_type")
    val bookmarkableType: BookmarkType,
) : Serializable

@Entity
@Table(name = "bookmarks")
class Bookmark(
    @EmbeddedId
    val id: BookmarkId,
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    val user: User,
) : AuditingEntity() {
    val bookmarkableId get() = id.bookmarkableId
    val bookmarkableType get() = id.bookmarkableType
}

enum class BookmarkType {
    POST,
    PROJECT,
}
