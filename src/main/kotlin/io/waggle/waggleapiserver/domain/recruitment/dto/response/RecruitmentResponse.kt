package io.waggle.waggleapiserver.domain.recruitment.dto.response

import io.waggle.waggleapiserver.domain.recruitment.Recruitment
import io.waggle.waggleapiserver.domain.user.enums.Position

class RecruitmentResponse(
    val recruitmentId: Long,
    val position: Position,
    val recruitingCount: Int,
) {
    companion object {
        fun from(recruitment: Recruitment): RecruitmentResponse =
            RecruitmentResponse(
                recruitmentId = recruitment.id,
                position = recruitment.position,
                recruitingCount = recruitment.recruitingCount,
            )
    }
}
