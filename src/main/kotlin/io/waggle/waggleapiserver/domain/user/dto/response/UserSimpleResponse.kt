package io.waggle.waggleapiserver.domain.user.dto.response

import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.enums.Position
import java.util.UUID

data class UserSimpleResponse(
    val userId: UUID,
    val username: String?,
    val email: String,
    val profileImageUrl: String?,
    val position: Position?,
    val yearCount: Int?,
) {
    companion object {
        fun from(user: User): UserSimpleResponse =
            UserSimpleResponse(
                user.id,
                user.username,
                user.email,
                user.profileImageUrl,
                user.position,
                user.yearCount,
            )
    }
}
