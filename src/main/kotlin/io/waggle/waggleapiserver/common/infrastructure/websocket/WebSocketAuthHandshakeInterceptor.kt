package io.waggle.waggleapiserver.common.infrastructure.websocket

import io.waggle.waggleapiserver.common.util.logger
import io.waggle.waggleapiserver.domain.auth.service.AuthService
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

@Component
class WebSocketAuthHandshakeInterceptor(
    private val authService: AuthService,
) : HandshakeInterceptor {
    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Boolean {
        val servletRequest =
            (request as? ServletServerHttpRequest)?.servletRequest ?: run {
                logger.warn("Failed to cast request to ServletServerHttpRequest")
                return false
            }

        val token = servletRequest.getParameter("token")
        if (token == null) {
            logger.warn("No WebSocket token found in request")
            return false
        }

        return try {
            val userId = authService.validateAndConsumeWsToken(token)
            attributes["userId"] = userId
            true
        } catch (e: Exception) {
            logger.warn("WebSocket token validation failed: ${e.message}")
            false
        }
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?,
    ) {
    }
}
