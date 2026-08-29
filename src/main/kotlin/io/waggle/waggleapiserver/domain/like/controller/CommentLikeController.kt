package io.waggle.waggleapiserver.domain.like.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.domain.like.LikeType
import io.waggle.waggleapiserver.domain.like.dto.response.LikeResponse
import io.waggle.waggleapiserver.domain.like.service.LikeService
import io.waggle.waggleapiserver.domain.user.User
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
    @Operation(summary = "댓글 좋아요")
    @PutMapping
    fun likeComment(
        @PathVariable commentId: Long,
        @CurrentUser user: User,
    ): LikeResponse = likeService.like(LikeType.COMMENT, commentId, user)

    @Operation(summary = "댓글 좋아요 취소")
    @DeleteMapping
    fun unlikeComment(
        @PathVariable commentId: Long,
        @CurrentUser user: User,
    ): LikeResponse = likeService.unlike(LikeType.COMMENT, commentId, user)
}
