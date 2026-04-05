package io.waggle.waggleapiserver.domain.message

import io.waggle.waggleapiserver.domain.message.dto.request.MessageSendRequest
import io.waggle.waggleapiserver.domain.message.service.MessageService
import io.waggle.waggleapiserver.security.oauth2.UserPrincipal
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import java.security.Principal

@MessageMapping("/message")
@Controller
class MessageStompController(
    private val messageService: MessageService,
) {
    @MessageMapping("/send")
    fun send(
        @Payload request: MessageSendRequest,
        principal: Principal,
    ) {
        val senderId = (principal as UserPrincipal).userId
        messageService.sendMessage(senderId, request)
    }
}
