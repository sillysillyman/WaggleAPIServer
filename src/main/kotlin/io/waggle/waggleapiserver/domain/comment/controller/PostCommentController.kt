package io.waggle.waggleapiserver.domain.comment.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.dto.response.CursorResponse
import io.waggle.waggleapiserver.common.infrastructure.persistence.AllowIncompleteSetup
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.domain.comment.dto.request.CommentCreateRequest
import io.waggle.waggleapiserver.domain.comment.dto.response.CommentResponse
import io.waggle.waggleapiserver.domain.comment.service.CommentService
import io.waggle.waggleapiserver.domain.user.User
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "댓글")
@RequestMapping("/posts/{postId}/comments")
@RestController
class PostCommentController(
    private val commentService: CommentService,
) {
    @Operation(summary = "댓글·답글 작성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createComment(
        @PathVariable postId: Long,
        @Valid @RequestBody request: CommentCreateRequest,
        @CurrentUser user: User,
    ): CommentResponse = commentService.createComment(postId, request, user)

    @AllowIncompleteSetup
    @Operation(summary = "댓글 목록 커서 페이지네이션 조회")
    @GetMapping
    fun getComments(
        @PathVariable postId: Long,
        @Valid @ParameterObject cursorQuery: CursorGetQuery,
        @CurrentUser user: User?,
    ): CursorResponse<CommentResponse> = commentService.getComments(postId, cursorQuery, user)
}
