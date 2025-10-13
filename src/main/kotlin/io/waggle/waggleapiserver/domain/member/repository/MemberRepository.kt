package io.waggle.waggleapiserver.domain.member.repository

import io.waggle.waggleapiserver.domain.member.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface MemberRepository : JpaRepository<Member, Long> {
    fun findByUserIdAndProjectId(
        userId: UUID,
        projectId: Long,
    ): Member?

    @Query(
        """
        SELECT m 
        FROM Member m 
        JOIN FETCH m.project 
        WHERE m.user.id = :userId 
        ORDER BY m.createdAt ASC
    """,
    )
    fun findAllByUserIdWithProjectOrderByCreatedAtAsc(
        @Param("userId") userId: UUID,
    ): List<Member>

    @Query(
        """
        SELECT m 
        FROM Member m 
        JOIN FETCH m.user 
        WHERE m.project.id = :projectId 
        ORDER BY m.createdAt ASC
    """,
    )
    fun findAllByProjectIdWithUserOrderByCreatedAtAsc(
        @Param("projectId") projectId: Long,
    ): List<Member>
}
