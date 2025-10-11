package io.waggle.waggleapiserver.security.oauth2

import io.waggle.waggleapiserver.security.jwt.JwtProvider
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class OAuth2LoginSuccessHandler(
    private val jwtProvider: JwtProvider,
) : SimpleUrlAuthenticationSuccessHandler() {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val userPrincipal = authentication.principal as UserPrincipal
        val userId = userPrincipal.user.id.toString()

        val accessToken = jwtProvider.generateAccessToken(userId)
        val refreshToken = jwtProvider.generateRefreshToken(userId)

        val targetUrl =
            UriComponentsBuilder
                .fromUriString("http://localhost:3000/oauth/callback")
                .queryParam("access_token", accessToken)
                .queryParam("refresh_token", refreshToken)
                .build()
                .toUriString()

        redirectStrategy.sendRedirect(request, response, targetUrl)
    }
}
