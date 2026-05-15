package io.waggle.waggleapiserver.domain.application.repository

import io.waggle.waggleapiserver.domain.application.ApplicationRead
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ApplicationReadRepository : JpaRepository<ApplicationRead, Long> {
    fun existsByApplicationIdAndUserId(
        applicationId: Long,
        userId: UUID,
    ): Boolean

    @Query(
        """
        SELECT ar.applicationId FROM ApplicationRead ar
        WHERE ar.userId = :userId
        AND ar.applicationId IN :applicationIds
        """,
    )
    fun findReadApplicationIds(
        userId: UUID,
        applicationIds: List<Long>,
    ): List<Long>

    fun findByApplicationIdInAndUserIdIn(
        applicationIds: List<Long>,
        userIds: List<UUID>,
    ): List<ApplicationRead>

    @Modifying
    @Query(
        """
        UPDATE application_reads SET deleted_at = UTC_TIMESTAMP(6)
        WHERE application_id = :applicationId AND deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByApplicationIdAndDeletedAtIsNull(applicationId: Long)

    @Modifying
    @Query(
        """
        UPDATE application_reads SET deleted_at = UTC_TIMESTAMP(6)
        WHERE user_id = :userId AND deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByUserIdAndDeletedAtIsNull(userId: UUID)

    @Modifying
    @Query(
        """
        UPDATE application_reads ar
        JOIN applications a ON a.id = ar.application_id
        SET ar.deleted_at = UTC_TIMESTAMP(6)
        WHERE a.post_id = :postId AND ar.deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByApplicationPostIdAndDeletedAtIsNull(postId: Long)

    @Modifying
    @Query(
        """
        UPDATE application_reads ar
        JOIN applications a ON a.id = ar.application_id
        SET ar.deleted_at = UTC_TIMESTAMP(6)
        WHERE a.team_id = :teamId AND ar.deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByApplicationTeamIdAndDeletedAtIsNull(teamId: Long)

    @Modifying
    @Query(
        """
        UPDATE application_reads ar
        JOIN applications a ON a.id = ar.application_id
        SET ar.deleted_at = UTC_TIMESTAMP(6)
        WHERE a.user_id = :userId AND ar.deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByApplicationUserIdAndDeletedAtIsNull(userId: UUID)

    @Modifying
    @Query(
        """
        UPDATE application_reads ar
        JOIN applications a ON a.id = ar.application_id
        JOIN posts p ON p.id = a.post_id
        SET ar.deleted_at = UTC_TIMESTAMP(6)
        WHERE p.user_id = :userId AND ar.deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByApplicationPostUserIdAndDeletedAtIsNull(userId: UUID)

    @Modifying
    @Query(
        """
        UPDATE application_reads ar
        JOIN applications a ON a.id = ar.application_id
        SET ar.deleted_at = UTC_TIMESTAMP(6)
        WHERE a.user_id = :userId
        AND a.team_id = :teamId
        AND a.id <> :excludedId
        AND a.status = 'PENDING'
        AND ar.deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByApplicationUserIdAndTeamIdAndApplicationIdNotAndStatusPendingAndDeletedAtIsNull(
        userId: UUID,
        teamId: Long,
        excludedId: Long,
    )
}
