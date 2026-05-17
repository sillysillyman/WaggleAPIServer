package io.waggle.waggleapiserver.domain.term.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

@Schema(description = "약관 동의 요청 DTO")
data class TermsAgreeRequest(
    @Schema(description = "약관 종류별 동의 여부 목록 (필수 약관 4종 모두 포함되어야 함)")
    @field:Valid
    @field:NotEmpty
    val agreements: List<TermAgreementRequest>,
)
