package io.waggle.waggleapiserver.domain.like.repository

import io.waggle.waggleapiserver.domain.like.Like
import io.waggle.waggleapiserver.domain.like.LikeId
import io.waggle.waggleapiserver.domain.like.LikeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface LikeRepository : JpaRepository<Like, LikeId> {
    fun countByIdTypeAndIdTargetId(
        type: LikeType,
        targetId: Long,
    ): Long

    @Query(
        """
        SELECT l.id.targetId AS targetId, COUNT(l) AS likeCount
        FROM Like l
        WHERE l.id.type = :type AND l.id.targetId IN :targetIds
        GROUP BY l.id.targetId
        """,
    )
    fun countLikesGroupByTargetId(
        type: LikeType,
        targetIds: List<Long>,
    ): List<TargetLikeCount>

    @Query(
        """
        SELECT l.id.targetId FROM Like l
        WHERE l.id.userId = :userId AND l.id.type = :type AND l.id.targetId IN :targetIds
        """,
    )
    fun findTargetIdsByUserIdAndTypeAndTargetIdIn(
        userId: UUID,
        type: LikeType,
        targetIds: List<Long>,
    ): List<Long>

    fun deleteByIdTypeAndIdTargetId(
        type: LikeType,
        targetId: Long,
    )

    fun deleteByIdUserId(userId: UUID)

    @Modifying
    @Query(
        """
        DELETE l FROM likes l
        JOIN comments c ON c.id = l.target_id
        WHERE l.type = 'COMMENT' AND c.post_id = :postId
        """,
        nativeQuery = true,
    )
    fun deleteByCommentPostId(postId: Long)

    @Modifying
    @Query(
        """
        DELETE l FROM likes l
        JOIN posts p ON p.id = l.target_id
        WHERE l.type = 'POST' AND p.team_id = :teamId
        """,
        nativeQuery = true,
    )
    fun deleteByPostTeamId(teamId: Long)

    @Modifying
    @Query(
        """
        DELETE l FROM likes l
        JOIN comments c ON c.id = l.target_id
        JOIN posts p ON p.id = c.post_id
        WHERE l.type = 'COMMENT' AND p.team_id = :teamId
        """,
        nativeQuery = true,
    )
    fun deleteByCommentPostTeamId(teamId: Long)

    @Modifying
    @Query(
        """
        DELETE l FROM likes l
        JOIN posts p ON p.id = l.target_id
        WHERE l.type = 'POST' AND p.user_id = :userId
        """,
        nativeQuery = true,
    )
    fun deleteByPostUserId(userId: UUID)

    @Modifying
    @Query(
        """
        DELETE l FROM likes l
        JOIN comments c ON c.id = l.target_id
        WHERE l.type = 'COMMENT' AND c.user_id = :userId
        """,
        nativeQuery = true,
    )
    fun deleteByCommentUserId(userId: UUID)
}

interface TargetLikeCount {
    val targetId: Long
    val likeCount: Long
}
