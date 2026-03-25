package io.waggle.waggleapiserver.domain.user.repository

import io.waggle.waggleapiserver.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun existsByEmail(email: String): Boolean

    fun existsByUsername(username: String): Boolean

    fun existsByIdAndDeletedAtIsNull(id: UUID): Boolean

    fun findByProviderAndProviderId(
        provider: String,
        providerId: String,
    ): User?

    @Query(
        """
        SELECT * FROM users
        WHERE provider = :provider
          AND provider_id = :providerId
          AND deleted_at IS NOT NULL
        """,
        nativeQuery = true,
    )
    fun findByProviderAndProviderIdAndDeletedAtIsNotNull(
        provider: String,
        providerId: String,
    ): User?
}
