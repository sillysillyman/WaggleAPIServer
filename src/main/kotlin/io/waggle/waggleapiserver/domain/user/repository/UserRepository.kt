package io.waggle.waggleapiserver.domain.user.repository

import io.waggle.waggleapiserver.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun existsByEmail(email: String): Boolean

    fun existsByUsername(username: String): Boolean

    fun findByEmail(email: String): User?

    fun findByProviderAndProviderId(
        provider: String,
        providerId: String,
    ): User?
}
