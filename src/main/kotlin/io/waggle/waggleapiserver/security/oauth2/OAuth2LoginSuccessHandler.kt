package io.waggle.waggleapiserver.security.oauth2

import io.waggle.waggleapiserver.domain.auth.service.AuthService
import io.waggle.waggleapiserver.domain.user.UserRole
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.util.UUID

@Component
class OAuth2LoginSuccessHandler(
    private val authService: AuthService,
    @Value("\${app.oauth2.redirect-uri}") private val redirectUri: String,
) : SimpleUrlAuthenticationSuccessHandler() {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val oauth2User = authentication.principal as OAuth2User

        val userId = UUID.fromString(oauth2User.getAttribute<String>("userId")!!)
        val role = UserRole.valueOf(oauth2User.getAttribute<String>("role")!!)

        val ott = authService.issueOttForOAuth(userId, role, response)

        val targetUrl =
            UriComponentsBuilder
                .fromUriString(redirectUri)
                .queryParam("ott", ott)
                .build()
                .toUriString()

        redirectStrategy.sendRedirect(request, response, targetUrl)
    }
}
