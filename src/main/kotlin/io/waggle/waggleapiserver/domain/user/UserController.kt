package io.waggle.waggleapiserver.domain.user

import io.waggle.waggleapiserver.common.util.CurrentUser
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkResponse
import io.waggle.waggleapiserver.domain.bookmark.service.BookmarkService
import io.waggle.waggleapiserver.domain.project.dto.response.ProjectSimpleResponse
import io.waggle.waggleapiserver.domain.project.service.ProjectService
import io.waggle.waggleapiserver.domain.user.dto.request.UserUpdateRequest
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import io.waggle.waggleapiserver.domain.user.service.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/users")
@RestController
class UserController(
    private val bookmarkService: BookmarkService,
    private val projectService: ProjectService,
    private val userService: UserService,
) {
    @GetMapping("/me/bookmarks")
    fun getUserBookmarks(
        @RequestParam bookmarkType: BookmarkType,
        @CurrentUser user: User,
    ): ResponseEntity<List<BookmarkResponse>> =
        ResponseEntity.ok(
            bookmarkService.getUserBookmarkables(
                bookmarkType,
                user,
            ),
        )

    @GetMapping("/{userId}/projects")
    fun getUserProjects(
        @PathVariable userId: UUID,
    ): ResponseEntity<List<ProjectSimpleResponse>> = ResponseEntity.ok(projectService.getUserProjects(userId))

    @GetMapping("/me/projects")
    fun getMyProjects(
        @CurrentUser user: User,
    ): ResponseEntity<List<ProjectSimpleResponse>> = ResponseEntity.ok(projectService.getUserProjects(user.id))

    @PutMapping("/me")
    fun updateMe(
        @Valid @RequestBody request: UserUpdateRequest,
        @CurrentUser user: User,
    ): ResponseEntity<UserSimpleResponse> =
        ResponseEntity.ok(
            userService.updateUser(
                user.id,
                request,
            ),
        )
}
