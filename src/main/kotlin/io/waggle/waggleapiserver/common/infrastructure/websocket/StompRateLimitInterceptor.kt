package io.waggle.waggleapiserver.common.infrastructure.websocket

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.waggle.waggleapiserver.common.dto.response.ErrorResponse
import io.waggle.waggleapiserver.security.oauth2.UserPrincipal
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpStatus
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class StompRateLimitInterceptor(
    @Lazy private val messagingTemplate: SimpMessagingTemplate,
) : ChannelInterceptor {
    // TODO: 다중 서버 환경 시 bucket4j-redis로 전환 필요
    private val buckets = ConcurrentHashMap<UUID, Bucket>()

    @Scheduled(fixedRate = 600_000)
    fun cleanUpExpiredBuckets() {
        buckets.entries.removeIf { it.value.availableTokens == SHORT_CAPACITY }
    }

    override fun preSend(
        message: Message<*>,
        channel: MessageChannel,
    ): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)
        if (accessor.command != StompCommand.SEND) return message
        if (accessor.destination != RATE_LIMITED_DESTINATION) return message

        val principal = accessor.user as? UserPrincipal ?: return null

        val bucket = buckets.computeIfAbsent(principal.userId) { createBucket() }
        if (bucket.tryConsume(1)) return message

        messagingTemplate.convertAndSendToUser(
            principal.userId.toString(),
            ERROR_DESTINATION,
            ErrorResponse(
                status = HttpStatus.TOO_MANY_REQUESTS.value(),
                code = "TOO_MANY_REQUESTS",
                message = "Too many messages. Please try again later.",
            ),
        )

        return null
    }

    private fun createBucket(): Bucket =
        Bucket
            .builder()
            .addLimit(
                Bandwidth
                    .builder()
                    .capacity(SHORT_CAPACITY)
                    .refillGreedy(SHORT_CAPACITY, SHORT_WINDOW)
                    .build(),
            ).addLimit(
                Bandwidth
                    .builder()
                    .capacity(LONG_CAPACITY)
                    .refillGreedy(LONG_CAPACITY, LONG_WINDOW)
                    .build(),
            ).build()

    companion object {
        private const val SHORT_CAPACITY = 30L
        private const val LONG_CAPACITY = 60L
        private val SHORT_WINDOW: Duration = Duration.ofSeconds(10)
        private val LONG_WINDOW: Duration = Duration.ofMinutes(1)
        private const val RATE_LIMITED_DESTINATION = "/app/message/send"
        private const val ERROR_DESTINATION = "/queue/errors"
    }
}
