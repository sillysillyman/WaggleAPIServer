package io.waggle.waggleapiserver.domain.follow.repository

import io.waggle.waggleapiserver.domain.follow.Follow
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FollowRepository : JpaRepository<Follow, Long> {
    fun existsByFollowerIdAndFolloweeId(
        followerId: UUID,
        followeeId: UUID,
    ): Boolean

    fun deleteByFollowerIdAndFolloweeId(
        followerId: UUID,
        followeeId: UUID,
    )

    fun countByFollowerId(followerId: UUID): Long

    fun countByFolloweeId(followeeId: UUID): Long

    fun findByFollowerId(followerId: UUID): List<Follow>

    fun findByFolloweeId(followeeId: UUID): List<Follow>
}
