package io.waggle.waggleapiserver.domain.like.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.domain.like.LikeType
import io.waggle.waggleapiserver.domain.like.dto.response.LikeResponse
import io.waggle.waggleapiserver.domain.like.service.LikeService
import io.waggle.waggleapiserver.domain.user.User
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "좋아요")
@RequestMapping("/posts/{postId}/like")
@RestController
class PostLikeController(
    private val likeService: LikeService,
) {
    @Operation(
        summary = "모집글 좋아요",
        responses = [
            ApiResponse(
                responseCode = "201",
                description = "좋아요 생성",
                content = [Content(schema = Schema(implementation = LikeResponse::class))],
            ),
            ApiResponse(
                responseCode = "200",
                description = "이미 좋아요한 상태",
                content = [Content(schema = Schema(implementation = LikeResponse::class))],
            ),
        ],
    )
    @PutMapping
    fun likePost(
        @PathVariable postId: Long,
        @CurrentUser user: User,
    ): ResponseEntity<LikeResponse> {
        val result = likeService.like(LikeType.POST, postId, user)
        return ResponseEntity
            .status(if (result.created) HttpStatus.CREATED else HttpStatus.OK)
            .body(result.response)
    }

    @Operation(summary = "모집글 좋아요 취소")
    @DeleteMapping
    fun unlikePost(
        @PathVariable postId: Long,
        @CurrentUser user: User,
    ): LikeResponse = likeService.unlike(LikeType.POST, postId, user)
}
