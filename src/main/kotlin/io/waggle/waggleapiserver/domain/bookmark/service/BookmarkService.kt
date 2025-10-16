package io.waggle.waggleapiserver.domain.bookmark.service

import io.waggle.waggleapiserver.domain.bookmark.Bookmark
import io.waggle.waggleapiserver.domain.bookmark.BookmarkId
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.dto.request.BookmarkToggleRequest
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkResponse
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkToggleResponse
import io.waggle.waggleapiserver.domain.bookmark.repository.BookmarkRepository
import io.waggle.waggleapiserver.domain.post.dto.response.PostSimpleResponse
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.project.dto.response.ProjectSimpleResponse
import io.waggle.waggleapiserver.domain.project.repository.ProjectRepository
import io.waggle.waggleapiserver.domain.user.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BookmarkService(
    private val bookmarkRepository: BookmarkRepository,
    private val postRepository: PostRepository,
    private val projectRepository: ProjectRepository,
) {
    fun toggleBookmark(
        request: BookmarkToggleRequest,
        user: User,
    ): BookmarkToggleResponse {
        val (bookmarkableId, bookmarkType) = request

        val bookmarkId =
            BookmarkId(
                userId = user.id,
                bookmarkableId = bookmarkableId,
                bookmarkType = bookmarkType,
            )
        return if (bookmarkRepository.existsById(bookmarkId)) {
            bookmarkRepository.deleteById(bookmarkId)
            BookmarkToggleResponse(false)
        } else {
            val bookmark = Bookmark(bookmarkId)
            bookmarkRepository.save(bookmark)
            BookmarkToggleResponse(true)
        }
    }

    fun getUserBookmarkables(
        bookmarkType: BookmarkType,
        user: User,
    ): List<BookmarkResponse> {
        val bookmarkableIds =
            bookmarkRepository
                .findByIdUserIdAndIdBookmarkType(user.id, bookmarkType)
                .map { it.bookmarkableId }

        return when (bookmarkType) {
            BookmarkType.POST -> {
                postRepository
                    .findByIdInOrderByCreatedAtDesc(bookmarkableIds)
                    .map { PostSimpleResponse.of(it, user) }
            }

            BookmarkType.PROJECT -> {
                projectRepository
                    .findByIdInOrderByCreatedAtDesc(bookmarkableIds)
                    .map { ProjectSimpleResponse.from(it) }
            }
        }
    }
}
