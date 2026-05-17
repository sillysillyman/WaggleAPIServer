package io.waggle.waggleapiserver.domain.post

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.dto.response.CursorResponse
import io.waggle.waggleapiserver.common.infrastructure.persistence.AllowIncompleteSetup
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.common.infrastructure.persistence.RequireCompleteSetup
import io.waggle.waggleapiserver.common.storage.dto.request.PresignedUrlRequest
import io.waggle.waggleapiserver.common.storage.dto.response.PresignedUrlResponse
import io.waggle.waggleapiserver.domain.post.dto.request.PostCreateRequest
import io.waggle.waggleapiserver.domain.post.dto.request.PostGetQuery
import io.waggle.waggleapiserver.domain.post.dto.request.PostUpdateRequest
import io.waggle.waggleapiserver.domain.post.dto.response.PostDetailResponse
import io.waggle.waggleapiserver.domain.post.dto.response.PostSimpleResponse
import io.waggle.waggleapiserver.domain.post.service.PostService
import io.waggle.waggleapiserver.domain.recruitment.dto.request.RecruitmentUpdateStatusRequest
import io.waggle.waggleapiserver.domain.user.User
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "모집글")
@RequestMapping("/posts")
@RestController
class PostController(
    private val postService: PostService,
) {
    @Operation(summary = "모집글 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPost(
        @Valid @RequestBody request: PostCreateRequest,
        @CurrentUser user: User,
    ): PostDetailResponse = postService.createPost(request, user)

    @RequireCompleteSetup
    @Operation(summary = "모집글 본문 이미지 업로드용 Presigned URL 생성")
    @PostMapping("/content-image/presigned-url")
    fun generateContentImagePresignedUrl(
        @Valid @RequestBody request: PresignedUrlRequest,
    ): PresignedUrlResponse = postService.generateContentImagePresignedUrl(request)

    @AllowIncompleteSetup
    @Operation(summary = "모집글 목록 커서 페이지네이션 조회")
    @GetMapping
    fun getPosts(
        @Valid @ParameterObject query: PostGetQuery,
        @Valid @ParameterObject cursorQuery: CursorGetQuery,
    ): CursorResponse<PostSimpleResponse> = postService.getPosts(query, cursorQuery)

    @AllowIncompleteSetup
    @Operation(summary = "모집글 상세 조회")
    @GetMapping("/{postId}")
    fun getPost(
        @PathVariable postId: Long,
        @CurrentUser user: User?,
    ): PostDetailResponse = postService.getPost(postId, user)

    @Operation(summary = "모집글 수정")
    @PutMapping("/{postId}")
    fun updatePost(
        @PathVariable postId: Long,
        @Valid @RequestBody request: PostUpdateRequest,
        @CurrentUser user: User,
    ): PostDetailResponse = postService.updatePost(postId, request, user)

    @Operation(summary = "모집 상태 변경")
    @PatchMapping("/{postId}/recruitment-status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updatePostRecruitmentStatus(
        @PathVariable postId: Long,
        @Valid @RequestBody request: RecruitmentUpdateStatusRequest,
        @CurrentUser user: User,
    ) = postService.updatePostRecruitmentStatus(postId, request, user)

    @Operation(summary = "모집글 삭제")
    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePost(
        @PathVariable postId: Long,
        @CurrentUser user: User,
    ) = postService.deletePost(postId, user)
}
