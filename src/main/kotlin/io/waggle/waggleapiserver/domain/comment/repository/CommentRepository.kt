package io.waggle.waggleapiserver.domain.comment.repository

import io.waggle.waggleapiserver.domain.comment.Comment
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface CommentRepository : JpaRepository<Comment, Long> {
    // 좋아요 대상 검증용. 파생 쿼리라 soft delete 필터가 적용되고, tombstone 검사까지 함께 처리함.
    fun existsByIdAndTombstonedAtIsNull(id: Long): Boolean

    fun existsByPostIdAndParentId(
        postId: Long,
        parentId: Long,
    ): Boolean

    // 목록에 렌더링되는 항목 수와 일치시킴. tombstone은 "삭제된 댓글입니다"로 보이므로 함께 집계.
    fun countByPostId(postId: Long): Long

    // 삭제-답글 경합으로 생기는 고아 답글 방지용.
    // 답글 작성 시 부모를, 삭제 시 대상을 이 메서드로 조회할 것.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockById(id: Long): Comment?

    @Query(
        """
        SELECT c FROM Comment c
        WHERE c.postId = :postId
        AND c.parentId IS NULL
        AND (:cursor IS NULL OR c.id > :cursor)
        ORDER BY c.id ASC
        """,
    )
    fun findRootsByPostIdWithCursor(
        postId: Long,
        cursor: Long?,
        pageable: Pageable,
    ): List<Comment>

    fun findByPostIdAndParentIdInOrderByParentIdAscIdAsc(
        postId: Long,
        parentIds: List<Long>,
    ): List<Comment>

    @Query(
        """
        SELECT c.postId AS postId, COUNT(c) AS commentCount
        FROM Comment c WHERE c.postId IN :postIds GROUP BY c.postId
        """,
    )
    fun countCommentsGroupByPostId(postIds: List<Long>): List<PostCommentCount>

    @Modifying
    @Query(
        """
        UPDATE comments SET deleted_at = UTC_TIMESTAMP(6)
        WHERE post_id = :postId AND deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByPostIdAndDeletedAtIsNull(postId: Long)

    @Modifying
    @Query(
        """
        UPDATE comments c JOIN posts p ON p.id = c.post_id
        SET c.deleted_at = UTC_TIMESTAMP(6)
        WHERE p.team_id = :teamId AND c.deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByPostInTeamIdAndDeletedAtIsNull(teamId: Long)

    @Modifying
    @Query(
        """
        UPDATE comments c JOIN comments r ON r.parent_id = c.id AND r.deleted_at IS NULL
        SET c.tombstoned_at = UTC_TIMESTAMP(6)
        WHERE c.user_id = :userId AND c.deleted_at IS NULL AND c.tombstoned_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateTombstonedAtByUserIdAndHasReply(userId: UUID)

    @Modifying
    @Query(
        """
        UPDATE comments c LEFT JOIN comments r ON r.parent_id = c.id AND r.deleted_at IS NULL
        SET c.deleted_at = UTC_TIMESTAMP(6)
        WHERE c.user_id = :userId AND c.deleted_at IS NULL AND r.id IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByUserIdAndHasNoReply(userId: UUID)

    // 반드시 위 두 쿼리 뒤에 실행할 것. mine에 deleted_at 조건이 없는 이유는
    // 이미 지워진 답글이라도 "있었다"는 사실로 부모를 찾아야 하기 때문.
    @Modifying
    @Query(
        """
        UPDATE comments p
        JOIN comments mine ON mine.parent_id = p.id AND mine.user_id = :userId
        LEFT JOIN comments alive ON alive.parent_id = p.id AND alive.deleted_at IS NULL
        SET p.deleted_at = UTC_TIMESTAMP(6)
        WHERE p.tombstoned_at IS NOT NULL AND p.deleted_at IS NULL AND alive.id IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByEmptiedTombstoneParentOfUserId(userId: UUID)
}

interface PostCommentCount {
    val postId: Long
    val commentCount: Long
}
