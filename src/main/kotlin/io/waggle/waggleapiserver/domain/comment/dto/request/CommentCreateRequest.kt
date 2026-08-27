package io.waggle.waggleapiserver.domain.comment.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

@Schema(description = "댓글 생성 요청 DTO")
data class CommentCreateRequest(
    @Schema(description = "부모 댓글 ID (답글이 아니면 null)", example = "10")
    @field:Positive
    val parentId: Long? = null,
    @Schema(description = "댓글 본문", example = "저 지원하고 싶은데요")
    @field:NotBlank
    @field:Size(max = 1000)
    val content: String,
)
