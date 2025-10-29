package io.waggle.waggleapiserver.domain.user.dto.request

import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.enums.Sido
import io.waggle.waggleapiserver.domain.user.enums.WorkTime
import io.waggle.waggleapiserver.domain.user.enums.WorkWay
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class UserUpdateRequest(
    @field:NotBlank val username: String,
    @field:NotNull val workTime: WorkTime,
    @field:NotNull val workWay: WorkWay,
    @field:NotNull val sido: Sido,
    @field:NotNull val position: Position,
    val yearCount: Int?,
    val detail: String?,
)
