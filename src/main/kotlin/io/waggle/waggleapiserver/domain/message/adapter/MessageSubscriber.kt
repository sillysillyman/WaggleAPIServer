package io.waggle.waggleapiserver.domain.message.adapter

import com.fasterxml.jackson.databind.ObjectMapper
import io.waggle.waggleapiserver.common.util.logger
import io.waggle.waggleapiserver.domain.message.dto.response.MessageResponse
import io.waggle.waggleapiserver.domain.message.repository.MessageRepository
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.data.redis.connection.MessageListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import org.springframework.data.redis.connection.Message as RedisMessage

@Component
class MessageSubscriber(
    private val objectMapper: ObjectMapper,
    private val messagingTemplate: SimpMessagingTemplate,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
) : MessageListener {
    override fun onMessage(
        redisMessage: RedisMessage,
        pattern: ByteArray?,
    ) {
        try {
            val payload = String(redisMessage.body)
            val event = objectMapper.readValue(payload, MessageEvent::class.java)

            val message =
                messageRepository
                    .findById(event.messageId)
                    .orElseThrow { IllegalStateException("Message not found: ${event.messageId}") }
            val sender =
                userRepository
                    .findById(message.senderId)
                    .orElseThrow { IllegalStateException("Sender not found: ${message.senderId}") }
            val receiver =
                userRepository
                    .findById(message.receiverId)
                    .orElseThrow { IllegalStateException("Receiver not found: ${message.receiverId}") }

            val response = MessageResponse.of(message, sender, receiver)

            messagingTemplate.convertAndSendToUser(
                event.receiverId.toString(),
                "/queue/messages",
                response,
            )
        } catch (e: Exception) {
            logger.error("Failed to process message event", e)
        }
    }
}
