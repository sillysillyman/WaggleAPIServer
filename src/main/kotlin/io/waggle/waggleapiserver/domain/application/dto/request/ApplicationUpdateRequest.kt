package io.waggle.waggleapiserver.domain.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL

@Schema(description = "팀 지원 수정 요청 DTO")
data class ApplicationUpdateRequest(
    @Schema(description = "팀 지원 동기")
    @field:Size(max = 1000)
    val detail: String? = null,
    @Schema(
        description = "포트폴리오 URL 목록",
        example = "[\"https://github.com/user\", \"https://blog.example.com\"]",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val portfolioUrls: List<@URL String> = emptyList(),
)
