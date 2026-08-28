package io.waggle.waggleapiserver.domain.user

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.AllowIncompleteSetup
import io.waggle.waggleapiserver.common.infrastructure.persistence.AllowMissingTermAgreement
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.common.storage.dto.request.PresignedUrlRequest
import io.waggle.waggleapiserver.common.storage.dto.response.PresignedUrlResponse
import io.waggle.waggleapiserver.domain.user.dto.request.UserSetupProfileRequest
import io.waggle.waggleapiserver.domain.user.dto.request.UserUpdateRequest
import io.waggle.waggleapiserver.domain.user.dto.response.UserCheckUsernameResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserDetailResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserProfileCompletionResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserProfileResponse
import io.waggle.waggleapiserver.domain.user.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
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
    private val userService: UserService,
) {
    @AllowIncompleteSetup
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

    @AllowMissingTermAgreement
    @Operation(summary = "본인 프로필 조회")
    @GetMapping("/me")
    fun getMyProfile(
        @CurrentUser user: User,
    ): UserProfileResponse = userService.getUserProfile(user)

    @AllowIncompleteSetup
    @Operation(summary = "프로필 완성 여부 조회")
    @GetMapping("/me/profile-completion")
    fun getMyProfileCompletion(
        @CurrentUser user: User,
    ): UserProfileCompletionResponse = userService.getUserProfileCompletion(user)

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
