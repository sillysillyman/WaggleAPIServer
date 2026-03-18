package io.waggle.waggleapiserver.domain.recruitment.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.recruitment.RecruitmentStatus
import jakarta.validation.constraints.NotNull

@Schema(description = "모집 상태 변경 요청 DTO")
data class RecruitmentUpdateStatusRequest(
    @Schema(description = "모집 상태", example = "CLOSED")
    @field:NotNull
    val status: RecruitmentStatus,
)
