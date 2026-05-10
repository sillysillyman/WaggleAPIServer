package io.waggle.waggleapiserver.domain.post.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.common.validation.constraint.UniquePosition
import io.waggle.waggleapiserver.domain.recruitment.dto.request.RecruitmentUpsertRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "모집글 생성 요청 DTO")
data class PostCreateRequest(
    @Schema(description = "팀 ID", example = "1")
    @field:NotNull
    val teamId: Long,
    @Schema(description = "모집글 제목")
    @field:NotBlank
    val title: String,
    @Schema(description = "모집글 내용")
    @field:NotBlank
    val content: String,
    @Schema(description = "모집 정보")
    @field:Valid
    @field:NotNull
    @field:UniquePosition
    val recruitments: List<RecruitmentUpsertRequest>,
)
