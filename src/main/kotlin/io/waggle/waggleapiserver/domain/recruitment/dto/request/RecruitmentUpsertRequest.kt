package io.waggle.waggleapiserver.domain.recruitment.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.enums.Skill
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

@Schema(description = "모집 생성/수정 요청 DTO")
data class RecruitmentUpsertRequest(
    @Schema(description = "모집 직무", example = "BACKEND")
    @field:NotNull
    val position: Position,
    @Schema(description = "모집 인원 수", example = "2")
    @field:NotNull
    val recruitingCount: Int,
    @Schema(description = "요구 스킬 목록", example = "[\"JAVA\", \"SPRING\"]")
    @field:NotEmpty
    val skills: Set<Skill>,
)
