package io.waggle.waggleapiserver.domain.notification.dto.request

import io.waggle.waggleapiserver.domain.notification.NotificationType

data class NotificationCreateRequest(
    val type: NotificationType,
    val redirectUrl: String,
    val contentArgs: List<String>,
) {
    companion object {
        fun of(
            type: NotificationType,
            redirectUrl: String,
            vararg contentArgs: String,
        ): NotificationCreateRequest = NotificationCreateRequest(type, redirectUrl, contentArgs.asList())
    }
}
