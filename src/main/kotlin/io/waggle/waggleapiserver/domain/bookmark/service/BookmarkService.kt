package io.waggle.waggleapiserver.domain.bookmark.service

import io.waggle.waggleapiserver.domain.bookmark.repository.BookmarkRepository
import org.springframework.stereotype.Service

@Service
class BookmarkService(
    private val bookmarkRepository: BookmarkRepository,
)
