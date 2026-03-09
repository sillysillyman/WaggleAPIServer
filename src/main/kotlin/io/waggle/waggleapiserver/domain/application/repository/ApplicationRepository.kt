package io.waggle.waggleapiserver.domain.application.repository

import io.waggle.waggleapiserver.domain.application.Application
import io.waggle.waggleapiserver.domain.user.enums.Position
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
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

    fun findByTeamId(teamId: Long): List<Application>

    fun findByUserId(userId: UUID): List<Application>

    fun findByPostId(postId: Long): List<Application>

    fun countByPostId(postId: Long): Int

    @Query("SELECT a.postId AS postId, COUNT(a) AS applicantCount FROM Application a WHERE a.postId IN :postIds GROUP BY a.postId")
    fun countApplicantsGroupByPostId(postIds: List<Long>): List<PostApplicantCount>
}

interface PostApplicantCount {
    val postId: Long
    val applicantCount: Long
}
