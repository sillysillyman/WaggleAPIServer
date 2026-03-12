package io.waggle.waggleapiserver.domain.recruitment.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.recruitment.Recruitment
import io.waggle.waggleapiserver.domain.recruitment.RecruitmentStatus
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.enums.Skill
import java.time.Instant

@Schema(description = "모집 정보 응답 DTO")
class RecruitmentResponse(
    @Schema(description = "모집 ID", example = "1")
    val recruitmentId: Long,
    @Schema(description = "모집 직무", example = "BACKEND")
    val position: Position,
    @Schema(description = "모집 인원 수", example = "3")
    val count: Int,
    @Schema(description = "모집 상태", example = "RECRUITING")
    val status: RecruitmentStatus,
    @Schema(description = "요구 스킬 목록")
    val skills: Set<Skill>,
    @Schema(description = "모집 생성일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
    @Schema(description = "모집 수정일시", example = "2025-11-16T12:30:45.123456Z")
    val updatedAt: Instant,
) {
    companion object {
        fun from(recruitment: Recruitment): RecruitmentResponse =
            RecruitmentResponse(
                recruitmentId = recruitment.id,
                position = recruitment.position,
                count = recruitment.count,
                status = recruitment.status,
                skills = recruitment.skills,
                createdAt = recruitment.createdAt,
                updatedAt = recruitment.updatedAt,
            )
    }
}
