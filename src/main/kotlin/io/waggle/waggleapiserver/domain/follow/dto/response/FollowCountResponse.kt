package io.waggle.waggleapiserver.domain.follow.dto.response

data class FollowCountResponse(
    val followedCount: Int,
    val followingCount: Int,
) {
    companion object {
        fun of(
            followedCount: Int,
            followingCount: Int,
        ) = FollowCountResponse(followedCount, followingCount)
    }
}
