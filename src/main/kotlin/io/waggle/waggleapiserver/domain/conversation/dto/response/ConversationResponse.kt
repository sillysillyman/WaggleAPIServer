package io.waggle.waggleapiserver.domain.conversation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.conversation.Conversation
import io.waggle.waggleapiserver.domain.message.Message
import io.waggle.waggleapiserver.domain.user.User
import java.time.Instant

@Schema(description = "대화방 응답 DTO")
data class ConversationResponse(
    @Schema(description = "대화 상대 정보")
    val partner: ConversationPartnerResponse,
    @Schema(description = "안 읽은 메시지 수", example = "3")
    val unreadCount: Long,
    @Schema(description = "마지막으로 읽은 메시지 ID (메시지 내역 조회 시 커서로 사용)", example = "147")
    val lastReadMessageId: Long?,
    @Schema(description = "마지막 메시지 정보")
    val lastMessage: LastMessage,
) {
    @Schema(description = "마지막 메시지 정보")
    data class LastMessage(
        @Schema(description = "메시지 ID", example = "150")
        val messageId: Long,
        @Schema(description = "메시지 내용", example = "내일 회의 가능?")
        val content: String,
        @Schema(description = "발송일시", example = "2025-11-16T12:30:45.123456Z")
        val createdAt: Instant,
    ) {
        companion object {
            fun from(message: Message): LastMessage =
                LastMessage(
                    messageId = message.id,
                    content = message.content,
                    createdAt = message.createdAt,
                )
        }
    }

    companion object {
        fun of(
            conversation: Conversation,
            partner: User?,
            lastMessage: Message,
        ): ConversationResponse =
            ConversationResponse(
                partner =
                    if (partner != null) {
                        ConversationPartnerResponse.from(partner)
                    } else {
                        ConversationPartnerResponse(
                            userId = conversation.partnerId,
                            username = null,
                            position = null,
                            profileImageUrl = null,
                        )
                    },
                unreadCount = conversation.unreadCount,
                lastReadMessageId = conversation.lastReadMessageId,
                lastMessage = LastMessage.from(lastMessage),
            )
    }
}
