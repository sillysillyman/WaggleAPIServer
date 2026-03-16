package io.waggle.waggleapiserver.domain.message

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.message.dto.request.MessageSendRequest
import io.waggle.waggleapiserver.domain.message.service.MessageService
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import java.util.UUID

@MessageMapping("/message")
@Controller
class MessageStompController(
    private val messageService: MessageService,
) {
    @MessageMapping(".send")
    fun send(
        @Header("simpSessionAttributes") attributes: Map<String, Any>,
        @Payload request: MessageSendRequest,
    ) {
        val senderId =
            attributes["userId"] as? UUID
                ?: throw BusinessException(ErrorCode.UNAUTHORIZED, "userId not found in session")
        messageService.sendMessage(senderId, request)
    }
}
