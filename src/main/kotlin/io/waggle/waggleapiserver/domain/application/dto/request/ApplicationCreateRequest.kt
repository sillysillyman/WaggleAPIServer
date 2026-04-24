package io.waggle.waggleapiserver.domain.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.common.validation.constraint.MaxBytes
import io.waggle.waggleapiserver.common.validation.constraint.WebUrl
import io.waggle.waggleapiserver.domain.user.enums.Position
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "팀 지원 요청 DTO")
data class ApplicationCreateRequest(
    @Schema(description = "모집글 ID", example = "1")
    @field:NotNull
    val postId: Long,
    @Schema(description = "지원 직무")
    @field:NotNull
    val position: Position,
    @Schema(description = "팀 지원 동기")
    @field:MaxBytes(1500)
    val detail: String? = null,
    @Schema(
        description = "포트폴리오 URL 목록",
        example = "[\"https://github.com/user\", \"https://blog.example.com\"]",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val portfolioUrls: List<
        @NotBlank @WebUrl
        String,
    > = emptyList(),
)
