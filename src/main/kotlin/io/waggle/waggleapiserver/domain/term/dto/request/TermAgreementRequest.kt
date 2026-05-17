package io.waggle.waggleapiserver.domain.term.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.term.TermType
import jakarta.validation.constraints.NotNull

@Schema(description = "약관 항목별 동의 여부 DTO")
data class TermAgreementRequest(
    @Schema(description = "약관 종류", example = "SERVICE")
    @field:NotNull
    val type: TermType,
    @Schema(description = "동의 여부 (필수 약관은 반드시 true)", example = "true")
    @field:NotNull
    val agreed: Boolean,
)
