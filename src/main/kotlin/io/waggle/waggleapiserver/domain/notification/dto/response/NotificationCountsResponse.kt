package io.waggle.waggleapiserver.domain.notification.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class NotificationCountsResponse(
    @Schema(description = "전체 알림 개수")
    val total: Long,
    @Schema(description = "안 읽은 알림 개수")
    val unread: Long,
)
