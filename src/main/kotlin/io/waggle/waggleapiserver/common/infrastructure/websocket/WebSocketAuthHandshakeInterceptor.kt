package io.waggle.waggleapiserver.common.infrastructure.websocket

import io.waggle.waggleapiserver.common.util.logger
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import io.waggle.waggleapiserver.security.jwt.JwtProvider
import io.waggle.waggleapiserver.security.jwt.JwtUtil
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

@Component
class WebSocketAuthHandshakeInterceptor(
    private val jwtProvider: JwtProvider,
    private val jwtUtil: JwtUtil,
    private val userRepository: UserRepository,
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

        // 헤더에서 토큰 추출 시도
        var token = jwtUtil.extractTokenFromRequest(servletRequest)

        // 헤더에 없으면 쿼리 파라미터에서 추출
        if (token == null) {
            token = servletRequest.getParameter("token")
            logger.info("Token extracted from query parameter")
        } else {
            logger.info("Token extracted from Authorization header")
        }

        // 토큰이 없거나 유효하지 않으면 연결 거부
        if (token == null) {
            logger.warn("No token found in request")
            return false
        }
        if (!jwtProvider.isTokenValid(token)) {
            logger.warn("Invalid token")
            return false
        }

        val userId = jwtProvider.getUserIdFromToken(token)

        // 실제 존재하는 유저인지 확인
        if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
            logger.warn("User not found: $userId")
            return false
        }

        attributes["userId"] = userId
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?,
    ) {
    }
}
