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

    fun findAllByUserIdOrderByCreatedAtAsc(userId: UUID): List<Member>

    fun findAllByProjectIdOrderByCreatedAtAsc(projectId: Long): List<Member>
}
