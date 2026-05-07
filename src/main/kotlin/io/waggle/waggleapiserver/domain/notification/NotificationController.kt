package io.waggle.waggleapiserver.domain.notification

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.dto.response.CursorResponse
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.domain.notification.dto.request.ReadNotificationsRequest
import io.waggle.waggleapiserver.domain.notification.dto.response.NotificationCountsResponse
import io.waggle.waggleapiserver.domain.notification.dto.response.NotificationResponse
import io.waggle.waggleapiserver.domain.notification.service.NotificationService
import io.waggle.waggleapiserver.domain.user.User
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "알림")
@RequestMapping("/notifications")
@RestController
class NotificationController(
    private val notificationService: NotificationService,
) {
    @Operation(summary = "알림 목록 조회")
    @GetMapping
    fun getNotifications(
        @ParameterObject cursorQuery: CursorGetQuery,
        @CurrentUser user: User,
    ): CursorResponse<NotificationResponse> = notificationService.getUserNotifications(cursorQuery, user)

    @Operation(summary = "알림 개수 조회")
    @GetMapping("/count")
    fun getNotificationCounts(
        @CurrentUser user: User,
    ): NotificationCountsResponse = notificationService.getNotificationCounts(user)

    @Operation(summary = "알림 읽음 처리")
    @PatchMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun readNotifications(
        @Valid @RequestBody request: ReadNotificationsRequest,
        @CurrentUser user: User,
    ) = notificationService.readNotifications(user, request.notificationIds)

    @Operation(summary = "알림 전체 읽음 처리")
    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun readAllNotifications(
        @CurrentUser user: User,
    ) = notificationService.readAllNotifications(user)
}
