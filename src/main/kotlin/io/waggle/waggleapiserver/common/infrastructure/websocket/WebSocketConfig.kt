package io.waggle.waggleapiserver.common.infrastructure.websocket

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val webSocketAuthHandshakeInterceptor: WebSocketAuthHandshakeInterceptor,
    private val webSocketAuthHandshakeHandler: WebSocketAuthHandshakeHandler,
    private val stompRateLimitInterceptor: StompRateLimitInterceptor,
    @Value("\${app.cors.allowed-origins}") private val allowedOrigins: List<String>,
) : WebSocketMessageBrokerConfigurer {
    @Bean
    fun webSocketHeartbeatScheduler(): ThreadPoolTaskScheduler =
        ThreadPoolTaskScheduler().apply {
            poolSize = 1
            setThreadNamePrefix("ws-heartbeat-")
        }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // REST CORS 와 목록이 갈리면 REST 는 되는데 메시지만 403 으로 끊김
        val originPatterns = allowedOrigins.toTypedArray()

        // 순수 WebSocket 엔드포인트
        registry
            .addEndpoint("/ws")
            .setAllowedOriginPatterns(*originPatterns)
            .addInterceptors(webSocketAuthHandshakeInterceptor)
            .setHandshakeHandler(webSocketAuthHandshakeHandler)

        // SockJS 폴백 엔드포인트
        registry
            .addEndpoint("/ws-sockjs")
            .setAllowedOriginPatterns(*originPatterns)
            .addInterceptors(webSocketAuthHandshakeInterceptor)
            .setHandshakeHandler(webSocketAuthHandshakeHandler)
            .withSockJS()
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.setApplicationDestinationPrefixes("/app")
        registry
            .enableSimpleBroker("/queue", "/topic")
            .setHeartbeatValue(longArrayOf(HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS))
            .setTaskScheduler(webSocketHeartbeatScheduler())
        registry.setUserDestinationPrefix("/user")
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(stompRateLimitInterceptor)
    }

    companion object {
        private const val HEARTBEAT_INTERVAL_MS = 10_000L
    }
}
