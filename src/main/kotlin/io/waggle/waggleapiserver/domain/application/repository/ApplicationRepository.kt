package io.waggle.waggleapiserver.domain.application.repository

import io.waggle.waggleapiserver.domain.application.Application
import io.waggle.waggleapiserver.domain.application.ApplicationStatus
import io.waggle.waggleapiserver.domain.user.enums.Position
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface ApplicationRepository : JpaRepository<Application, Long> {
    fun existsByTeamIdAndUserIdAndPosition(
        teamId: Long,
        userId: UUID,
        position: Position,
    ): Boolean

    fun findByIdAndUserId(
        id: Long,
        userId: UUID,
    ): Application?

    fun findByUserId(userId: UUID): List<Application>

    fun findByStatusAndCreatedAtBetween(
        status: ApplicationStatus,
        createdAtStart: Instant,
        createdAtEnd: Instant,
    ): List<Application>

    @Query(
        """
        SELECT a FROM Application a
        WHERE a.teamId = :teamId
        AND (
            :cursor IS NULL
            OR a.statusPriority > :cursorStatusPriority
            OR (a.statusPriority = :cursorStatusPriority AND a.id < :cursor)
        )
        ORDER BY a.statusPriority, a.id DESC
        """,
    )
    fun findByTeamIdWithCursor(
        teamId: Long,
        cursor: Long?,
        cursorStatusPriority: Int?,
        pageable: Pageable,
    ): List<Application>

    @Query(
        """
        SELECT a FROM Application a
        WHERE a.postId = :postId
        AND (
            :cursor IS NULL
            OR a.statusPriority > :cursorStatusPriority
            OR (a.statusPriority = :cursorStatusPriority AND a.id < :cursor)
        )
        ORDER BY a.statusPriority, a.id DESC
        """,
    )
    fun findByPostIdWithCursor(
        postId: Long,
        cursor: Long?,
        cursorStatusPriority: Int?,
        pageable: Pageable,
    ): List<Application>

    @Query("SELECT a.postId AS postId, COUNT(a) AS applicantCount FROM Application a WHERE a.postId IN :postIds GROUP BY a.postId")
    fun countApplicantsGroupByPostId(postIds: List<Long>): List<PostApplicantCount>

    @Query(
        """
        SELECT a.postId AS postId, COUNT(a) AS unreadCount
        FROM Application a
        WHERE a.postId IN :postIds
        AND NOT EXISTS (
            SELECT 1 FROM ApplicationRead ar
            WHERE ar.applicationId = a.id
            AND ar.userId = :userId
        )
        GROUP BY a.postId
        """,
    )
    fun countUnreadApplicationsGroupByPostId(
        userId: UUID,
        postIds: List<Long>,
    ): List<PostUnreadCount>

    @Modifying
    @Query(
        """
        UPDATE applications SET deleted_at = UTC_TIMESTAMP(6)
        WHERE user_id = :userId AND deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByUserIdAndDeletedAtIsNull(userId: UUID)
}

interface PostApplicantCount {
    val postId: Long
    val applicantCount: Long
}

interface PostUnreadCount {
    val postId: Long
    val unreadCount: Long
}
