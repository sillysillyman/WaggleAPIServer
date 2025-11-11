package io.waggle.waggleapiserver.domain.user

import io.waggle.waggleapiserver.common.util.CurrentUser
import io.waggle.waggleapiserver.domain.application.dto.response.ApplicationResponse
import io.waggle.waggleapiserver.domain.application.service.ApplicationService
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkResponse
import io.waggle.waggleapiserver.domain.bookmark.service.BookmarkService
import io.waggle.waggleapiserver.domain.follow.dto.response.FollowCountResponse
import io.waggle.waggleapiserver.domain.follow.service.FollowService
import io.waggle.waggleapiserver.domain.notification.dto.response.NotificationResponse
import io.waggle.waggleapiserver.domain.notification.service.NotificationService
import io.waggle.waggleapiserver.domain.project.dto.response.ProjectSimpleResponse
import io.waggle.waggleapiserver.domain.user.dto.request.UserUpdateRequest
import io.waggle.waggleapiserver.domain.user.dto.response.UserDetailResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import io.waggle.waggleapiserver.domain.user.service.UserService
import jakarta.validation.Valid
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
    private val applicationService: ApplicationService,
    private val bookmarkService: BookmarkService,
    private val followService: FollowService,
    private val notificationService: NotificationService,
    private val userService: UserService,
) {
    @GetMapping("/{userId}")
    fun getUser(
        @PathVariable userId: UUID,
    ): UserDetailResponse = userService.getUser(userId)

    @GetMapping("/{userId}/follow-count")
    fun getUserFollowCount(
        @PathVariable userId: UUID,
    ): FollowCountResponse = followService.getUserFollowCount(userId)

    @GetMapping("/me/bookmarks")
    fun getMyBookmarks(
        @RequestParam bookmarkType: BookmarkType,
        @CurrentUser user: User,
    ): List<BookmarkResponse> = bookmarkService.getUserBookmarkables(bookmarkType, user)

    @GetMapping("/{userId}/projects")
    fun getUserProjects(
        @PathVariable userId: UUID,
    ): List<ProjectSimpleResponse> = userService.getUserProjects(userId)

    @GetMapping("/me/applications")
    fun getMyApplications(
        @CurrentUser user: User,
    ): List<ApplicationResponse> = applicationService.getUserApplications(user.id)

    @GetMapping("/me/followees")
    fun getMyFollowees(
        @CurrentUser user: User,
    ): List<UserSimpleResponse> = followService.getUserFollowees(user.id)

    @GetMapping("/me/followers")
    fun getMyFollowers(
        @CurrentUser user: User,
    ): List<UserSimpleResponse> = followService.getUserFollowers(user.id)

    @GetMapping("/me/notifications")
    fun getMyNotifications(
        @CurrentUser user: User,
    ): List<NotificationResponse> = notificationService.getUserNotifications(user.id)

    @GetMapping("/me/projects")
    fun getMyProjects(
        @CurrentUser user: User,
    ): List<ProjectSimpleResponse> = userService.getUserProjects(user.id)

    @PutMapping("/me")
    fun updateMe(
        @Valid @RequestBody request: UserUpdateRequest,
        @CurrentUser user: User,
    ): UserDetailResponse = userService.updateUser(user.id, request)
}
