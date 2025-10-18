package io.waggle.waggleapiserver.domain.recruitment.dto.request

import io.waggle.waggleapiserver.domain.user.enums.Position
import jakarta.validation.constraints.NotNull

data class RecruitmentUpsertRequest(
    @field:NotNull val position: Position,
    @field:NotNull val recruitingCount: Int,
)
