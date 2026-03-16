package io.waggle.waggleapiserver.domain.message

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.dto.response.CursorResponse
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.domain.message.dto.response.MessageResponse
import io.waggle.waggleapiserver.domain.message.service.MessageService
import io.waggle.waggleapiserver.domain.user.User
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "메시지")
@RestController
@RequestMapping("/messages")
class MessageController(
    val messageService: MessageService,
) {
    @Operation(summary = "메시지 내역 조회")
    @GetMapping("/{partnerId}")
    fun getMessageHistory(
        @PathVariable partnerId: UUID,
        @ParameterObject cursorQuery: CursorGetQuery,
        @CurrentUser user: User,
    ): CursorResponse<MessageResponse> = messageService.getMessageHistory(partnerId, cursorQuery, user)
}
