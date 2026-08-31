package io.waggle.waggleapiserver.domain.bookmark.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkResponse
import io.waggle.waggleapiserver.domain.bookmark.service.BookmarkService
import io.waggle.waggleapiserver.domain.post.dto.response.BookmarkedPostResponse
import io.waggle.waggleapiserver.domain.team.dto.response.TeamResponse
import io.waggle.waggleapiserver.domain.user.User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "북마크")
@RequestMapping("/users/me/bookmarks")
@RestController
class UserBookmarkController(
    private val bookmarkService: BookmarkService,
) {
    @Operation(
        summary = "본인 북마크 목록 조회",
        responses = [
            ApiResponse(
                responseCode = "200",
                content = [
                    Content(
                        array =
                            ArraySchema(
                                schema = Schema(oneOf = [BookmarkedPostResponse::class, TeamResponse::class]),
                            ),
                    ),
                ],
            ),
        ],
    )
    @GetMapping
    fun getMyBookmarks(
        @RequestParam type: BookmarkType,
        @CurrentUser user: User,
    ): List<BookmarkResponse> = bookmarkService.getUserBookmarkables(type, user)
}
