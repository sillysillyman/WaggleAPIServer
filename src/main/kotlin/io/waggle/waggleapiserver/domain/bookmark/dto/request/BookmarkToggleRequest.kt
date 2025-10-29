package io.waggle.waggleapiserver.domain.bookmark.dto.request

import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import jakarta.validation.constraints.NotNull

data class BookmarkToggleRequest(
    @field:NotNull val bookmarkableId: Long,
    @field:NotNull val bookmarkType: BookmarkType,
)
