package io.waggle.waggleapiserver.security.oauth2

import io.waggle.waggleapiserver.domain.user.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

class UserPrincipal(
    private val oauth2User: OAuth2User,
    val user: User,
) : OAuth2User {
    override fun getName() = user.id.toString()

    override fun getAttributes(): Map<String, Any> = oauth2User.attributes

    override fun getAuthorities(): Collection<GrantedAuthority> = oauth2User.authorities
}
