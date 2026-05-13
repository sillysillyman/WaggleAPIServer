package io.waggle.waggleapiserver.domain.auth.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "OAuth OTT 교환 요청 DTO")
data class OttRedeemRequest(
    @Schema(description = "OAuth 콜백에서 받은 1회용 토큰")
    @field:NotBlank
    val ott: String,
)
