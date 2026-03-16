package io.waggle.waggleapiserver.domain.message.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.message.Message
import io.waggle.waggleapiserver.domain.user.User
import java.time.Instant

@Schema(description = "메시지 응답 DTO")
data class MessageResponse(
    @Schema(description = "메시지 ID", example = "1")
    val messageId: Long,
    @Schema(description = "발신자 정보")
    val sender: MessagePartnerResponse,
    @Schema(description = "수신자 정보")
    val receiver: MessagePartnerResponse,
    @Schema(description = "메시지 내용", example = "Hello, World!")
    val content: String,
    @Schema(description = "발송일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
    @Schema(description = "확인일시", example = "2025-11-30T12:30:45.123456Z")
    val readAt: Instant?,
) {
    companion object {
        fun of(
            message: Message,
            sender: User,
            receiver: User,
        ): MessageResponse =
            MessageResponse(
                messageId = message.id,
                sender = MessagePartnerResponse.from(sender),
                receiver = MessagePartnerResponse.from(receiver),
                content = message.content,
                createdAt = message.createdAt,
                readAt = message.readAt,
            )
    }
}
