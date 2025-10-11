package io.waggle.waggleapiserver.domain.user.dto.response

import io.waggle.waggleapiserver.domain.user.User
import java.util.UUID

data class UserSimpleResponse(
    val userId: UUID,
    val username: String?,
    val email: String,
    val profileImageUrl: String?,
) {
    companion object {
        fun from(user: User): UserSimpleResponse =
            UserSimpleResponse(
                user.id,
                user.username,
                user.email,
                user.profileImageUrl,
            )
    }
}
