package io.waggle.waggleapiserver.domain.bookmark.service

import io.waggle.waggleapiserver.domain.bookmark.Bookmark
import io.waggle.waggleapiserver.domain.bookmark.BookmarkId
import io.waggle.waggleapiserver.domain.bookmark.dto.request.BookmarkToggleRequest
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkToggleResponse
import io.waggle.waggleapiserver.domain.bookmark.repository.BookmarkRepository
import io.waggle.waggleapiserver.domain.user.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookmarkService(
    private val bookmarkRepository: BookmarkRepository,
) {
    @Transactional
    fun toggleBookmark(
        request: BookmarkToggleRequest,
        user: User,
    ): BookmarkToggleResponse {
        val bookmarkId =
            BookmarkId(
                bookmarkableId = request.bookmarkableId,
                bookmarkType = request.bookmarkType,
                userId = user.id,
            )
        return if (bookmarkRepository.existsById(bookmarkId)) {
            bookmarkRepository.deleteById(bookmarkId)
            BookmarkToggleResponse(false)
        } else {
            val bookmark =
                Bookmark(
                    id = bookmarkId,
                    user = user,
                )
            bookmarkRepository.save(bookmark)
            BookmarkToggleResponse(true)
        }
    }
}
