package io.waggle.waggleapiserver.domain.bookmark.repository

import io.waggle.waggleapiserver.domain.bookmark.Bookmark
import io.waggle.waggleapiserver.domain.bookmark.BookmarkId
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BookmarkRepository : JpaRepository<Bookmark, BookmarkId> {
    fun countByIdTargetIdAndIdType(
        targetId: Long,
        type: BookmarkType,
    ): Int

    fun findByIdUserIdAndIdType(
        userId: UUID,
        type: BookmarkType,
    ): List<Bookmark>

    fun deleteByIdUserId(userId: UUID)
}
