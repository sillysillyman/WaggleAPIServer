package io.waggle.waggleapiserver.security.oauth2

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class OAuth2LoginFailureHandler(
    @Value("\${app.oauth2.redirect-uri}") private val redirectUri: String,
) : SimpleUrlAuthenticationFailureHandler() {
    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        val errorCode = findBusinessErrorCode(exception)?.name ?: ErrorCode.UNAUTHORIZED.name

        val targetUrl =
            UriComponentsBuilder
                .fromUriString(redirectUri)
                .queryParam("errorCode", errorCode)
                .build()
                .toUriString()

        redirectStrategy.sendRedirect(request, response, targetUrl)
    }

    private fun findBusinessErrorCode(throwable: Throwable?): ErrorCode? {
        var current = throwable
        while (current != null) {
            if (current is BusinessException) return current.errorCode
            current = current.cause
        }
        return null
    }
}
