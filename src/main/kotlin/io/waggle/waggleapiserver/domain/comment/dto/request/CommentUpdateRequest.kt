package io.waggle.waggleapiserver.domain.comment.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "댓글 수정 요청 DTO")
data class CommentUpdateRequest(
    @Schema(description = "댓글 본문", example = "저 지원하고 싶은데요")
    @field:NotBlank
    @field:Size(max = 1000)
    val content: String,
)
