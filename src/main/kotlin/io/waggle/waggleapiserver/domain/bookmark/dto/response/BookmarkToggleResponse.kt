package io.waggle.waggleapiserver.domain.bookmark.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "북마크 토글 응답 DTO")
data class BookmarkToggleResponse(
    @Schema(description = "북마크 여부", example = "true")
    val bookmarked: Boolean,
)
