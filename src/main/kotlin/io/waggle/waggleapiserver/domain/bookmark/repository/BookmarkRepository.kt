package io.waggle.waggleapiserver.domain.bookmark.repository

import io.waggle.waggleapiserver.domain.bookmark.Bookmark
import io.waggle.waggleapiserver.domain.bookmark.BookmarkId
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
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

    fun deleteByIdTargetIdAndIdType(
        targetId: Long,
        type: BookmarkType,
    )

    @Modifying
    @Query(
        """
        DELETE b FROM bookmarks b
        JOIN posts p ON p.id = b.target_id
        WHERE b.type = 'POST' AND p.team_id = :teamId
        """,
        nativeQuery = true,
    )
    fun deleteByPostTeamId(teamId: Long)

    @Modifying
    @Query(
        """
        DELETE b FROM bookmarks b
        JOIN posts p ON p.id = b.target_id
        WHERE b.type = 'POST' AND p.user_id = :userId
        """,
        nativeQuery = true,
    )
    fun deleteByPostUserId(userId: UUID)
}
