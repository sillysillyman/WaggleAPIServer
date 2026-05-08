package io.waggle.waggleapiserver.domain.follow.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "팔로우 개수 응답 DTO")
data class FollowCountsResponse(
    @Schema(description = "해당 계정을 팔로우하는 개수", example = "100")
    val followed: Int,
    @Schema(description = "해당 계정이 팔로우하는 개수", example = "100")
    val following: Int,
)
