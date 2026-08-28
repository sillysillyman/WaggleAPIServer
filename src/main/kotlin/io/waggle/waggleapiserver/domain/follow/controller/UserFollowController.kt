package io.waggle.waggleapiserver.domain.follow.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.domain.follow.dto.response.FollowCountsResponse
import io.waggle.waggleapiserver.domain.follow.service.FollowService
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "팔로우")
@RequestMapping("/users")
@RestController
class UserFollowController(
    private val followService: FollowService,
) {
    @Operation(summary = "사용자 팔로우 개수 정보 조회")
    @GetMapping("/{userId}/follow-count")
    fun getUserFollowCounts(
        @PathVariable userId: UUID,
    ): FollowCountsResponse = followService.getUserFollowCounts(userId)

    @Operation(summary = "본인이 팔로우 하는 계정 목록 조회")
    @GetMapping("/me/followees")
    fun getMyFollowees(
        @CurrentUser user: User,
    ): List<UserSimpleResponse> = followService.getUserFollowees(user.id)

    @Operation(summary = "본인을 팔로우 하는 계정 목록 조회")
    @GetMapping("/me/followers")
    fun getMyFollowers(
        @CurrentUser user: User,
    ): List<UserSimpleResponse> = followService.getUserFollowers(user.id)
}
