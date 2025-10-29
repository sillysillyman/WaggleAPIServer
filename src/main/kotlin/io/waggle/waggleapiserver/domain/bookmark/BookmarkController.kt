package io.waggle.waggleapiserver.domain.bookmark

import io.waggle.waggleapiserver.common.util.CurrentUser
import io.waggle.waggleapiserver.domain.bookmark.dto.request.BookmarkToggleRequest
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkToggleResponse
import io.waggle.waggleapiserver.domain.bookmark.service.BookmarkService
import io.waggle.waggleapiserver.domain.user.User
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/bookmarks")
@RestController
class BookmarkController(
    private val bookmarkService: BookmarkService,
) {
    @PostMapping
    fun toggleBookmark(
        @Valid @RequestBody request: BookmarkToggleRequest,
        @CurrentUser user: User,
    ): ResponseEntity<BookmarkToggleResponse> = ResponseEntity.ok(bookmarkService.toggleBookmark(request, user))
}
