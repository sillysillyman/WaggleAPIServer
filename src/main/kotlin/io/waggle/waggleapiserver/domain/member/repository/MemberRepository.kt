package io.waggle.waggleapiserver.domain.member.repository

import io.waggle.waggleapiserver.domain.member.Member
import io.waggle.waggleapiserver.domain.member.MemberRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface MemberRepository : JpaRepository<Member, Long> {
    fun existsByUserIdAndTeamId(
        userId: UUID,
        teamId: Long,
    ): Boolean

    fun findByUserIdAndTeamIdIn(
        userId: UUID,
        teamIds: List<Long>,
    ): List<Member>

    fun countByTeamId(teamId: Long): Int

    @Query(
        """
        SELECT m.teamId AS teamId, COUNT(m) AS count
        FROM Member m
        WHERE m.teamId IN :teamIds
        GROUP BY m.teamId
        """,
    )
    fun countByTeamIds(teamIds: List<Long>): List<TeamMemberCount>

    fun findByUserIdAndTeamId(
        userId: UUID,
        teamId: Long,
    ): Member?

    @Query(
        """
        SELECT * FROM members
        WHERE user_id = :userId AND team_id = :teamId AND deleted_at IS NOT NULL
        """,
        nativeQuery = true,
    )
    fun findByUserIdAndTeamIdAndDeletedAtIsNotNull(
        userId: UUID,
        teamId: Long,
    ): Member?

    @Query(
        """
        SELECT * FROM members
        WHERE user_id = :userId AND team_id = :teamId
        """,
        nativeQuery = true,
    )
    fun findByUserIdAndTeamIdIncludingDeleted(
        userId: UUID,
        teamId: Long,
    ): Member?

    fun findByTeamId(teamId: Long): List<Member>

    fun findByTeamIdAndUserIdNot(
        teamId: Long,
        userId: UUID,
    ): List<Member>

    fun findByIdNotAndTeamIdOrderByRoleAscCreatedAtAsc(
        id: Long,
        teamId: Long,
    ): List<Member>

    fun findByUserIdOrderByRoleAscCreatedAtAsc(userId: UUID): List<Member>

    fun findByUserIdAndVisibleTrueOrderByRoleAscCreatedAtAsc(userId: UUID): List<Member>

    fun findByTeamIdOrderByRoleAscCreatedAtAsc(teamId: Long): List<Member>

    fun findByTeamIdAndRoleIn(
        teamId: Long,
        roles: List<MemberRole>,
    ): List<Member>

    @Query(
        """
        SELECT * FROM members
        WHERE team_id = :teamId AND deleted_at IS NOT NULL
        ORDER BY role ASC, created_at ASC
        """,
        nativeQuery = true,
    )
    fun findByTeamIdAndDeletedAtIsNotNullOrderByRoleAscCreatedAtAsc(teamId: Long): List<Member>

    @Modifying
    @Query(
        """
        UPDATE members SET deleted_at = UTC_TIMESTAMP(6), deleted_by = :userId
        WHERE user_id = :userId AND deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtAndDeletedByByUserIdAndDeletedAtIsNull(userId: UUID)

    @Modifying
    @Query(
        """
        UPDATE members SET deleted_at = UTC_TIMESTAMP(6), deleted_by = :deletedBy
        WHERE team_id = :teamId AND deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtAndDeletedByByTeamIdAndDeletedAtIsNull(
        teamId: Long,
        deletedBy: UUID,
    )
}

interface TeamMemberCount {
    val teamId: Long
    val count: Long
}
