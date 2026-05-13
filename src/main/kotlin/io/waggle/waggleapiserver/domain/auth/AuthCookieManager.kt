package io.waggle.waggleapiserver.domain.auth

import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AuthCookieManager(
    @Value("\${app.cookie.secure}") private val cookieSecure: Boolean,
    @Value("\${app.cookie.same-site}") private val cookieSameSite: String,
    @Value("\${app.cookie.domain}") private val cookieDomain: String?,
) {
    fun addRefreshTokenCookie(
        response: HttpServletResponse,
        token: String,
        maxAgeSeconds: Int,
    ) {
        response.addHeader("Set-Cookie", buildCookieHeader(token, maxAgeSeconds))
    }

    fun expireRefreshTokenCookie(response: HttpServletResponse) {
        response.addHeader("Set-Cookie", buildCookieHeader("", 0))
    }

    private fun buildCookieHeader(
        value: String,
        maxAgeSeconds: Int,
    ): String =
        buildString {
            append("refreshToken=$value; HttpOnly; Path=/auth; Max-Age=$maxAgeSeconds; ")
            if (cookieSecure) append("Secure; ")
            cookieDomain?.takeIf { it.isNotBlank() }?.let { append("Domain=$it; ") }
            append("SameSite=$cookieSameSite")
        }
}
