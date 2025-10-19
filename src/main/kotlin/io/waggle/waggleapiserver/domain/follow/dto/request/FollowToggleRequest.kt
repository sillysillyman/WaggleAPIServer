package io.waggle.waggleapiserver.domain.follow.dto.request

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class FollowToggleRequest(
    @field:NotNull val userId: UUID,
)
