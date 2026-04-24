package io.waggle.waggleapiserver.domain.post.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.common.validation.constraint.MaxBytes
import io.waggle.waggleapiserver.domain.recruitment.dto.request.RecruitmentUpsertRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Schema(description = "모집글 생성/수정 요청 DTO")
data class PostUpsertRequest(
    @Schema(description = "팀 ID", example = "1")
    @field:NotNull
    val teamId: Long,
    @Schema(description = "모집글 제목")
    @field:NotBlank
    @field:MaxBytes(30)
    val title: String,
    @Schema(description = "모집글 내용")
    @field:NotBlank
    @field:MaxBytes(15000)
    val content: String,
    @Schema(description = "모집 정보")
    @field:NotNull
    @field:Size(max = 6)
    @field:Valid
    val recruitments: List<RecruitmentUpsertRequest>,
)
