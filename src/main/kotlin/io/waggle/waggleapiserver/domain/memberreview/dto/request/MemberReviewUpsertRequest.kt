package io.waggle.waggleapiserver.domain.memberreview.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.memberreview.enums.ReviewTag
import io.waggle.waggleapiserver.domain.memberreview.enums.ReviewType
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Schema(description = "팀원 리뷰 작성/수정 요청 DTO")
data class MemberReviewUpsertRequest(
    @Schema(description = "리뷰 타입", example = "LIKE")
    @field:NotNull
    val type: ReviewType,
    @Schema(description = "리뷰 태그 (최소 1개, 최대 3개)")
    @field:NotNull
    @field:Size(min = 1, max = 3)
    val tags: Set<ReviewTag>,
)
