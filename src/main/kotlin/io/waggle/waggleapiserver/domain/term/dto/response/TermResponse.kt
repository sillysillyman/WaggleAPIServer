package io.waggle.waggleapiserver.domain.term.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.term.Term
import io.waggle.waggleapiserver.domain.term.TermType

@Schema(description = "약관 응답 DTO")
data class TermResponse(
    @Schema(description = "약관 ID", example = "1")
    val id: Long,
    @Schema(description = "약관 종류", example = "SERVICE")
    val type: TermType,
    @Schema(description = "약관 버전", example = "1")
    val version: Int,
    @Schema(description = "약관 본문 URL", example = "https://www.notion.so/...")
    val contentUrl: String,
    @Schema(description = "필수 동의 여부", example = "true")
    val mandatory: Boolean,
    @Schema(description = "현재 사용자의 동의 여부 (비로그인 시 false)", example = "false")
    val agreed: Boolean,
) {
    companion object {
        fun of(
            term: Term,
            agreed: Boolean,
        ): TermResponse =
            TermResponse(
                id = term.id,
                type = term.type,
                version = term.version,
                contentUrl = term.contentUrl,
                mandatory = term.mandatory,
                agreed = agreed,
            )
    }
}
