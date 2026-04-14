package io.waggle.waggleapiserver.domain.conversation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.User
import java.util.UUID

@Schema(description = "대화 상대 정보")
data class ConversationPartnerResponse(
    @Schema(description = "사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val userId: UUID,
    @Schema(description = "사용자명", example = "testUser")
    val username: String?,
    @Schema(description = "직무", example = "BACKEND")
    val position: Position?,
    @Schema(description = "프로필 이미지 URL", example = "https://example.com/image.png")
    val profileImageUrl: String?,
) {
    companion object {
        fun from(user: User): ConversationPartnerResponse =
            ConversationPartnerResponse(
                userId = user.id,
                username = user.username,
                position = user.position,
                profileImageUrl = user.profileImageUrl,
            )
    }
}
