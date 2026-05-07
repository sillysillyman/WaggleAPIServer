package io.waggle.waggleapiserver.domain.user

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.AllowIncompleteProfile
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.common.storage.dto.request.PresignedUrlRequest
import io.waggle.waggleapiserver.common.storage.dto.response.PresignedUrlResponse
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkResponse
import io.waggle.waggleapiserver.domain.bookmark.service.BookmarkService
import io.waggle.waggleapiserver.domain.follow.dto.response.FollowCountResponse
import io.waggle.waggleapiserver.domain.follow.service.FollowService
import io.waggle.waggleapiserver.domain.post.dto.response.PostSimpleResponse
import io.waggle.waggleapiserver.domain.team.dto.response.TeamResponse
import io.waggle.waggleapiserver.domain.team.dto.response.UserTeamResponse
import io.waggle.waggleapiserver.domain.user.dto.request.MemberUpdateVisibilityRequest
import io.waggle.waggleapiserver.domain.user.dto.request.UserSetupProfileRequest
import io.waggle.waggleapiserver.domain.user.dto.request.UserUpdateRequest
import io.waggle.waggleapiserver.domain.user.dto.response.UserCheckUsernameResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserDetailResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserProfileCompletionResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserProfileResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import io.waggle.waggleapiserver.domain.user.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "사용자")
@RequestMapping("/users")
@RestController
class UserController(
    private val bookmarkService: BookmarkService,
    private val followService: FollowService,
    private val userService: UserService,
) {
    @AllowIncompleteProfile
    @Operation(summary = "사용자 프로필 초기 설정")
    @PostMapping("/me/profile")
    fun setupProfile(
        @Valid @RequestBody request: UserSetupProfileRequest,
        @CurrentUser user: User,
    ): UserDetailResponse = userService.setupProfile(request, user)

    @Operation(summary = "사용자 프로필 이미지 업로드용 Presigned URL 생성")
    @PostMapping("/me/profile-image/presigned-url")
    fun generateProfileImagePresignedUrl(
        @Valid @RequestBody request: PresignedUrlRequest,
    ): PresignedUrlResponse = userService.generateProfileImagePresignedUrl(request)

    @Operation(summary = "사용자명 사용 가능 여부 조회")
    @GetMapping("/check")
    fun checkUsername(
        @RequestParam username: String,
    ): UserCheckUsernameResponse = userService.checkUsername(username)

    @Operation(summary = "사용자 조회")
    @GetMapping("/{userId}")
    fun getUser(
        @PathVariable userId: UUID,
    ): UserProfileResponse = userService.getUserProfile(userId)

    @Operation(summary = "사용자 팔로우 개수 정보 조회")
    @GetMapping("/{userId}/follow-count")
    fun getUserFollowCount(
        @PathVariable userId: UUID,
    ): FollowCountResponse = followService.getUserFollowCount(userId)

    @Operation(summary = "사용자 참여 팀 목록 조회")
    @GetMapping("/{userId}/teams")
    fun getUserTeams(
        @PathVariable userId: UUID,
    ): List<UserTeamResponse> = userService.getUserTeams(userId, includeHidden = false)

    @Operation(summary = "본인 프로필 조회")
    @GetMapping("/me")
    fun getMyProfile(
        @CurrentUser user: User,
    ): UserProfileResponse = userService.getUserProfile(user)

    @Operation(
        summary = "본인 북마크 목록 조회",
        responses = [
            ApiResponse(
                responseCode = "200",
                content = [
                    Content(
                        array = ArraySchema(schema = Schema(oneOf = [PostSimpleResponse::class, TeamResponse::class])),
                    ),
                ],
            ),
        ],
    )
    @GetMapping("/me/bookmarks")
    fun getMyBookmarks(
        @RequestParam type: BookmarkType,
        @CurrentUser user: User,
    ): List<BookmarkResponse> = bookmarkService.getUserBookmarkables(type, user)

    @Operation(summary = "본인이 팔로우 하는 계정 목록 조회")
    @GetMapping("/me/followees")
    fun getMyFollowees(
        @CurrentUser user: User,
    ): List<UserSimpleResponse> = followService.getUserFollowees(user.id)

    @Operation(summary = "본인을 팔로우 하는 계정 목록 조회")
    @GetMapping("/me/followers")
    fun getMyFollowers(
        @CurrentUser user: User,
    ): List<UserSimpleResponse> = followService.getUserFollowers(user.id)

    @AllowIncompleteProfile
    @Operation(summary = "프로필 완성 여부 조회")
    @GetMapping("/me/profile-completion")
    fun getMyProfileCompletion(
        @CurrentUser user: User,
    ): UserProfileCompletionResponse = userService.getUserProfileCompletion(user)

    @Operation(summary = "본인 참여 팀 목록 조회")
    @GetMapping("/me/teams")
    fun getMyTeams(
        @CurrentUser user: User,
    ): List<UserTeamResponse> = userService.getUserTeams(user.id, includeHidden = true)

    @Operation(summary = "본인 팀 공개/비공개 설정")
    @PatchMapping("/me/teams/{teamId}/visibility")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateMyTeamVisibility(
        @PathVariable teamId: Long,
        @Valid @RequestBody request: MemberUpdateVisibilityRequest,
        @CurrentUser user: User,
    ) = userService.updateTeamVisibility(user.id, teamId, request)

    @Operation(summary = "본인 프로필 수정")
    @PutMapping("/me")
    fun updateMe(
        @Valid @RequestBody request: UserUpdateRequest,
        @CurrentUser user: User,
    ): UserDetailResponse = userService.updateUser(request, user)

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deactivateMe(
        @CurrentUser user: User,
    ) = userService.deactivateUser(user)
}
