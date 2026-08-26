package io.waggle.waggleapiserver.domain.comment.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.domain.comment.dto.request.CommentUpdateRequest
import io.waggle.waggleapiserver.domain.comment.dto.response.CommentResponse
import io.waggle.waggleapiserver.domain.comment.service.CommentService
import io.waggle.waggleapiserver.domain.user.User
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "댓글")
@RequestMapping("/comments")
@RestController
class CommentController(
    private val commentService: CommentService,
) {
    @Operation(summary = "댓글 수정")
    @PutMapping("/{commentId}")
    fun updateComment(
        @PathVariable commentId: Long,
        @Valid @RequestBody request: CommentUpdateRequest,
        @CurrentUser user: User,
    ): CommentResponse = commentService.updateComment(commentId, request, user)

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteComment(
        @PathVariable commentId: Long,
        @CurrentUser user: User,
    ) = commentService.deleteComment(commentId, user)
}
