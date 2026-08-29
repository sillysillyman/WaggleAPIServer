package io.waggle.waggleapiserver.domain.like.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "좋아요 응답 DTO")
data class LikeResponse(
    @Schema(description = "좋아요 여부", example = "true")
    val liked: Boolean,
    @Schema(description = "좋아요 수", example = "42")
    val likeCount: Long,
) {
    companion object {
        fun of(
            liked: Boolean,
            likeCount: Long,
        ): LikeResponse =
            LikeResponse(
                liked = liked,
                likeCount = likeCount,
            )
    }
}
