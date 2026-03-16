package io.waggle.waggleapiserver.domain.message.service

import io.waggle.waggleapiserver.common.dto.request.CursorDirection
import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.dto.response.CursorResponse
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.conversation.service.ConversationService
import io.waggle.waggleapiserver.domain.message.Message
import io.waggle.waggleapiserver.domain.message.adapter.MessageEvent
import io.waggle.waggleapiserver.domain.message.adapter.MessagePublisher
import io.waggle.waggleapiserver.domain.message.dto.request.MessageSendRequest
import io.waggle.waggleapiserver.domain.message.dto.response.MessageResponse
import io.waggle.waggleapiserver.domain.message.repository.MessageRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MessageService(
    private val conversationService: ConversationService,
    private val messagePublisher: MessagePublisher,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun sendMessage(
        senderId: UUID,
        request: MessageSendRequest,
    ) {
        val (receiverId, content) = request

        if (!userRepository.existsById(receiverId)) {
            throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Receiver not found: $receiverId")
        }

        val message =
            Message(
                senderId = senderId,
                receiverId = receiverId,
                content = content,
            )
        val savedMessage = messageRepository.save(message)

        conversationService.updateConversations(savedMessage)

        val event =
            MessageEvent(
                messageId = savedMessage.id,
                receiverId = receiverId,
            )
        messagePublisher.publish(event)
    }

    @Transactional(readOnly = true)
    fun getMessageHistory(
        partnerId: UUID,
        cursorQuery: CursorGetQuery,
        user: User,
    ): CursorResponse<MessageResponse> {
        val (cursor, size, direction) = cursorQuery
        val pageable = PageRequest.of(0, size + 1)

        val messages =
            when (direction ?: CursorDirection.BEFORE) {
                CursorDirection.BEFORE ->
                    messageRepository.findMessageHistoryBefore(user.id, partnerId, cursor, pageable)
                CursorDirection.AFTER ->
                    messageRepository.findMessageHistoryAfter(user.id, partnerId, cursor, pageable)
            }

        val hasNext = messages.size > size
        val slicedMessages = if (hasNext) messages.take(size) else messages

        val partner = userRepository.findById(partnerId)
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Partner not found: $partnerId") }
        val userById = mapOf(user.id to user, partner.id to partner)

        val data = slicedMessages.map { msg ->
            MessageResponse.of(
                message = msg,
                sender = userById[msg.senderId]!!,
                receiver = userById[msg.receiverId]!!,
            )
        }
        val nextCursor = if (hasNext) slicedMessages.last().id else null

        return CursorResponse(
            data = data,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }
}
