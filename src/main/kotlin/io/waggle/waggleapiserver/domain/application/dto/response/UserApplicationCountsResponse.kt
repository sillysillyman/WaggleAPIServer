package io.waggle.waggleapiserver.domain.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "본인 지원 상태별 개수 응답 DTO")
data class UserApplicationCountsResponse(
    @Schema(description = "전체 지원 개수", example = "85")
    val total: Long,
    @Schema(description = "PENDING 개수", example = "10")
    val pending: Long,
    @Schema(description = "APPROVED 개수", example = "5")
    val approved: Long,
    @Schema(description = "REJECTED 개수", example = "70")
    val rejected: Long,
)
