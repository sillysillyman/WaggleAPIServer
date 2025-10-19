package io.waggle.waggleapiserver.domain.follow.service

import io.waggle.waggleapiserver.domain.follow.Follow
import io.waggle.waggleapiserver.domain.follow.dto.request.FollowToggleRequest
import io.waggle.waggleapiserver.domain.follow.dto.response.FollowCountResponse
import io.waggle.waggleapiserver.domain.follow.repository.FollowRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class FollowService(
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun toggleFollow(
        request: FollowToggleRequest,
        user: User,
    ): Boolean {
        val followeeId = request.userId
        val followerId = user.id

        if (followeeId == user.id) {
            throw IllegalArgumentException("Cannot follow yourself")
        }

        if (userRepository.existsById(followeeId)) {
            throw EntityNotFoundException("User not found: $followeeId")
        }

        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId)
            return false
        }

        val follow =
            Follow(
                followerId = followerId,
                followeeId = followeeId,
            )
        followRepository.save(follow)

        return true
    }

    fun getUserFollowees(userId: UUID): List<UserSimpleResponse> {
        val follows = followRepository.findByFollowerId(userId)

        val userIds = follows.map { it.followerId }
        val userMap = userRepository.findAllById(userIds).associateBy { it.id }

        return follows.map { follow ->
            val user =
                userMap[follow.followerId]
                    ?: throw EntityNotFoundException("User not found: ${follow.followerId}")
            UserSimpleResponse.from(user)
        }
    }

    fun getUserFollowers(userId: UUID): List<UserSimpleResponse> {
        val follows = followRepository.findByFolloweeId(userId)

        val userIds = follows.map { it.followeeId }
        val userMap = userRepository.findAllById(userIds).associateBy { it.id }

        return follows.map { follow ->
            val user =
                userMap[follow.followeeId]
                    ?: throw EntityNotFoundException("User not found: ${follow.followeeId}")
            UserSimpleResponse.from(user)
        }
    }

    fun getUserFollowCount(userId: UUID): FollowCountResponse =
        FollowCountResponse.of(
            followRepository.countByFolloweeId(userId).toInt(),
            followRepository.countByFollowerId(userId).toInt(),
        )
}
