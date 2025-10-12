package io.waggle.waggleapiserver.domain.bookmark

import io.waggle.waggleapiserver.domain.bookmark.service.BookmarkService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/bookmarks")
@RestController
class BookmarkController(
    private val bookmarkService: BookmarkService,
)
