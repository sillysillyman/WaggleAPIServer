package io.waggle.waggleapiserver.domain.post

import io.waggle.waggleapiserver.common.dto.PageResponse
import io.waggle.waggleapiserver.common.dto.SingleItemResponse
import io.waggle.waggleapiserver.common.util.CurrentUser
import io.waggle.waggleapiserver.domain.post.dto.request.PostSearchQuery
import io.waggle.waggleapiserver.domain.post.dto.request.PostUpsertRequest
import io.waggle.waggleapiserver.domain.post.dto.response.PostDetailResponse
import io.waggle.waggleapiserver.domain.post.dto.response.PostSimpleResponse
import io.waggle.waggleapiserver.domain.post.service.PostService
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.security.oauth2.UserPrincipal
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/posts")
@RestController
class PostController(
    private val postService: PostService,
) {
    @PostMapping
    fun createPost(
        @Valid @RequestBody request: PostUpsertRequest,
        @CurrentUser user: User,
    ): ResponseEntity<SingleItemResponse<PostDetailResponse>> {
        postService.createPost(request, user)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(null)
    }

    @GetMapping
    fun getPosts(
        @ModelAttribute query: PostSearchQuery,
        @CurrentUser user: User?,
        @PageableDefault(
            size = 15,
            sort = ["createdAt"],
            direction = Sort.Direction.DESC,
        ) pageable: Pageable,
    ): ResponseEntity<PageResponse<PostSimpleResponse>> = ResponseEntity.ok(PageResponse.of(postService.getPosts(query, pageable)))

    @GetMapping("/{postId}")
    fun getPost(
        @PathVariable postId: Long,
        @CurrentUser user: User?,
    ): ResponseEntity<SingleItemResponse<PostDetailResponse>> = ResponseEntity.ok(SingleItemResponse(postService.getPost(postId)))

    @PutMapping("/{postId}")
    fun updatePost(
        @PathVariable postId: Long,
        @Valid @RequestBody request: PostUpsertRequest,
        @CurrentUser user: User,
    ) {
        postService.updatePost(postId, request, user)
    }

    @DeleteMapping("/{postId}")
    fun deletePost(
        @PathVariable postId: Long,
        @CurrentUser user: User,
    ) {
        postService.deletePost(postId, user)
    }
}
