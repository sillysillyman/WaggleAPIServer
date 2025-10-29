package io.waggle.waggleapiserver.security.oauth2

import io.waggle.waggleapiserver.domain.user.UserRole
import java.security.Principal
import java.util.UUID

class UserPrincipal(
    val userId: UUID,
    val email: String,
    val role: UserRole,
) : Principal {
    override fun getName() = userId.toString()
}
