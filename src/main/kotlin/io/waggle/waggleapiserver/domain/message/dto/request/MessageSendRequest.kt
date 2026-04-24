package io.waggle.waggleapiserver.domain.message.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.common.validation.constraint.MaxBytes
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Schema(description = "메시지 발신 요청 DTO")
data class MessageSendRequest(
    @Schema(description = "수신자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    @field:NotNull
    val receiverId: UUID,
    @Schema(description = "메시지 내용")
    @field:NotBlank
    @field:MaxBytes(6000)
    val content: String,
)
