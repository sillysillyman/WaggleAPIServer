package io.waggle.waggleapiserver.common.infrastructure.websocket

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
) : WebSocketMessageBrokerConfigurer {
    @Bean
    fun webSocketHeartbeatScheduler(): ThreadPoolTaskScheduler =
        ThreadPoolTaskScheduler().apply {
            poolSize = 1
            setThreadNamePrefix("ws-heartbeat-")
        }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // 순수 WebSocket 엔드포인트
        registry
            .addEndpoint("/ws")
            .setAllowedOriginPatterns(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://waggle.lol",
            ).addInterceptors(webSocketAuthHandshakeInterceptor)
            .setHandshakeHandler(webSocketAuthHandshakeHandler)

        // SockJS 폴백 엔드포인트
        registry
            .addEndpoint("/ws-sockjs")
            .setAllowedOriginPatterns(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://waggle.lol",
            ).addInterceptors(webSocketAuthHandshakeInterceptor)
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
