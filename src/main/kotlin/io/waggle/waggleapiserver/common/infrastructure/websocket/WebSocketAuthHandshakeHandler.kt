package io.waggle.waggleapiserver.common.infrastructure.websocket

import io.waggle.waggleapiserver.domain.user.UserRole
import io.waggle.waggleapiserver.security.oauth2.UserPrincipal
import org.springframework.http.server.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import java.security.Principal
import java.util.UUID

@Component
class WebSocketAuthHandshakeHandler : DefaultHandshakeHandler() {
    override fun determineUser(
        request: ServerHttpRequest,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Principal? {
        val userId = attributes["userId"] as? UUID ?: return null
        return UserPrincipal(userId = userId, role = UserRole.USER)
    }
}
