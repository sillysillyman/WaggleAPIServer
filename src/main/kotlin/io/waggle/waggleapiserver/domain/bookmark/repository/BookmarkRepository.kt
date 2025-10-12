package io.waggle.waggleapiserver.domain.bookmark.repository

import io.waggle.waggleapiserver.domain.bookmark.Bookmark
import io.waggle.waggleapiserver.domain.bookmark.BookmarkId
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BookmarkRepository : JpaRepository<Bookmark, BookmarkId> {
    fun countByBookmarkableIdAndBookmarkType(
        bookmarkableId: Long,
        bookmarkType: BookmarkType,
    ): Int

    fun findByIdUserIdAndIdBookmarkType(
        userId: UUID,
        bookmarkType: BookmarkType,
    ): List<Bookmark>
}
