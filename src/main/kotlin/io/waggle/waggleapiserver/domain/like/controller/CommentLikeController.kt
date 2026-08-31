package io.waggle.waggleapiserver.domain.like.controller

import io.swagger.v3.oas.annotations.Operation
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
@RequestMapping("/comments/{commentId}/like")
@RestController
class CommentLikeController(
    private val likeService: LikeService,
) {
    @Operation(
        summary = "댓글 좋아요",
        description = "멱등. 최초 생성 시 201, 이미 좋아요한 상태면 200.",
    )
    @PutMapping
    fun likeComment(
        @PathVariable commentId: Long,
        @CurrentUser user: User,
    ): ResponseEntity<LikeResponse> {
        val result = likeService.like(LikeType.COMMENT, commentId, user)
        return ResponseEntity
            .status(if (result.created) HttpStatus.CREATED else HttpStatus.OK)
            .body(result.response)
    }

    @Operation(
        summary = "댓글 좋아요 취소",
        description = "멱등. 좋아요하지 않은 상태에서 호출해도 200.",
    )
    @DeleteMapping
    fun unlikeComment(
        @PathVariable commentId: Long,
        @CurrentUser user: User,
    ): LikeResponse = likeService.unlike(LikeType.COMMENT, commentId, user)
}
