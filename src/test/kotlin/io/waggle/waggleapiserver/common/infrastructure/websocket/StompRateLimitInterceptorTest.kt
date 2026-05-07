package io.waggle.waggleapiserver.common.infrastructure.websocket

import io.waggle.waggleapiserver.common.dto.response.ErrorResponse
import io.waggle.waggleapiserver.domain.user.UserRole
import io.waggle.waggleapiserver.security.oauth2.UserPrincipal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import java.security.Principal
import java.util.UUID

class StompRateLimitInterceptorTest {
    private lateinit var messagingTemplate: SpyMessagingTemplate
    private lateinit var interceptor: StompRateLimitInterceptor
    private lateinit var channel: MessageChannel

    @BeforeEach
    fun setUp() {
        messagingTemplate = SpyMessagingTemplate()
        interceptor = StompRateLimitInterceptor(messagingTemplate)
        channel = MessageChannel { _, _ -> true }
    }

    @Test
    fun `SEND가 아닌 프레임은 그대로 통과`() {
        val message = stompMessage(StompCommand.SUBSCRIBE, destination = "/queue/messages", user = randomUser())

        val result = interceptor.preSend(message, channel)

        assertThat(result).isSameAs(message)
        assertThat(messagingTemplate.sentMessages).isEmpty()
    }

    @Test
    fun `SEND라도 rate limit 대상 destination이 아니면 그대로 통과`() {
        val message = stompMessage(StompCommand.SEND, destination = "/app/other", user = randomUser())

        val result = interceptor.preSend(message, channel)

        assertThat(result).isSameAs(message)
        assertThat(messagingTemplate.sentMessages).isEmpty()
    }

    @Test
    fun `인증되지 않은 SEND는 에러 알림 없이 drop`() {
        val message = stompMessage(StompCommand.SEND, destination = "/app/message/send", user = null)

        val result = interceptor.preSend(message, channel)

        assertThat(result).isNull()
        assertThat(messagingTemplate.sentMessages).isEmpty()
    }

    @Test
    fun `한도 이내의 SEND는 모두 통과`() {
        val user = randomUser()

        repeat(60) {
            val message = stompMessage(StompCommand.SEND, destination = "/app/message/send", user = user)
            val result = interceptor.preSend(message, channel)
            assertThat(result).isSameAs(message)
        }
        assertThat(messagingTemplate.sentMessages).isEmpty()
    }

    @Test
    fun `한도 초과 시 drop되고 사용자에게 에러 알림 전송`() {
        val user = randomUser()
        repeat(60) {
            interceptor.preSend(
                stompMessage(StompCommand.SEND, destination = "/app/message/send", user = user),
                channel,
            )
        }

        val overflow = stompMessage(StompCommand.SEND, destination = "/app/message/send", user = user)
        val result = interceptor.preSend(overflow, channel)

        assertThat(result).isNull()
        assertThat(messagingTemplate.sentMessages).hasSize(1)
        val sent = messagingTemplate.sentMessages.first()
        assertThat(sent.user).isEqualTo(user.userId.toString())
        assertThat(sent.destination).isEqualTo("/queue/errors")
        val payload = sent.payload as ErrorResponse
        assertThat(payload.status).isEqualTo(429)
        assertThat(payload.code).isEqualTo("TOO_MANY_REQUESTS")
    }

    @Test
    fun `사용자별 버킷이 독립적으로 관리됨`() {
        val userA = randomUser()
        val userB = randomUser()
        repeat(60) {
            interceptor.preSend(
                stompMessage(StompCommand.SEND, destination = "/app/message/send", user = userA),
                channel,
            )
        }

        val userBMessage = stompMessage(StompCommand.SEND, destination = "/app/message/send", user = userB)
        val result = interceptor.preSend(userBMessage, channel)

        assertThat(result).isSameAs(userBMessage)
        assertThat(messagingTemplate.sentMessages).isEmpty()
    }

    private fun randomUser() = UserPrincipal(userId = UUID.randomUUID(), role = UserRole.USER)

    private fun stompMessage(
        command: StompCommand,
        destination: String,
        user: Principal?,
    ): Message<*> {
        val accessor = StompHeaderAccessor.create(command)
        accessor.destination = destination
        accessor.user = user
        return MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
    }

    private data class CapturedMessage(
        val user: String,
        val destination: String,
        val payload: Any,
    )

    private class SpyMessagingTemplate : SimpMessagingTemplate(MessageChannel { _, _ -> true }) {
        val sentMessages = mutableListOf<CapturedMessage>()

        override fun convertAndSendToUser(
            user: String,
            destination: String,
            payload: Any,
        ) {
            sentMessages += CapturedMessage(user, destination, payload)
        }
    }
}
