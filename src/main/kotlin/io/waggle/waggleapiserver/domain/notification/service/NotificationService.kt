package io.waggle.waggleapiserver.domain.notification.service

import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.dto.response.CursorResponse
import io.waggle.waggleapiserver.domain.notification.NotificationType
import io.waggle.waggleapiserver.domain.notification.dto.response.NotificationCountsResponse
import io.waggle.waggleapiserver.domain.notification.dto.response.NotificationResponse
import io.waggle.waggleapiserver.domain.notification.repository.NotificationRepository
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.team.repository.TeamRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val postRepository: PostRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
) {
    fun getUserNotifications(
        cursorQuery: CursorGetQuery,
        user: User,
    ): CursorResponse<NotificationResponse> {
        val pageable = PageRequest.of(0, cursorQuery.size + 1)
        val notifications =
            notificationRepository.findByUserIdWithCursor(user.id, cursorQuery.cursor, pageable)

        val hasNext = notifications.size > cursorQuery.size
        val slicedNotifications =
            if (hasNext) notifications.take(cursorQuery.size) else notifications

        val teamIds =
            slicedNotifications
                .mapNotNull { (it.metadata["teamId"] as? Number)?.toLong() }
                .distinct()
        val teamById = teamRepository.findAllById(teamIds).associateBy { it.id }

        val postIds =
            slicedNotifications
                .mapNotNull { (it.metadata["postId"] as? Number)?.toLong() }
                .distinct()
        val postById = postRepository.findAllById(postIds).associateBy { it.id }

        val triggeredBys =
            slicedNotifications
                .mapNotNull {
                    (it.metadata["triggeredBy"] as? String)?.let(UUID::fromString)
                }.distinct()
        val triggeredByUserById =
            userRepository.findAllById(triggeredBys).associateBy { it.id }

        val data =
            slicedNotifications.map { notification ->
                val teamId = (notification.metadata["teamId"] as? Number)?.toLong()
                val team =
                    teamId?.let { teamById[it] }?.let { NotificationResponse.TeamResponse.from(it) }

                val postId = (notification.metadata["postId"] as? Number)?.toLong()
                val post =
                    postId?.let { postById[it] }?.let { NotificationResponse.PostResponse.from(it) }

                val triggeredByUserId =
                    (notification.metadata["triggeredBy"] as? String)
                        ?.let(UUID::fromString)
                val triggeredBy =
                    triggeredByUserId
                        ?.let { triggeredByUserById[it] }
                        ?.let { NotificationResponse.TriggeredByResponse.from(it) }

                val metadata =
                    buildMap<String, Any?> {
                        putAll(notification.metadata.filterKeys { it !in HYDRATED_METADATA_KEYS })
                        team?.let { put("team", it) }
                        post?.let { put("post", it) }
                        if (notification.type in TRIGGERED_BY_TYPES) {
                            triggeredBy?.let { put("triggeredBy", it) }
                        }
                    }.ifEmpty { null }

                NotificationResponse.of(notification = notification, metadata = metadata)
            }

        return CursorResponse(
            data = data,
            nextCursor = if (hasNext) slicedNotifications.lastOrNull()?.id else null,
            hasNext = hasNext,
        )
    }

    fun getNotificationCounts(user: User): NotificationCountsResponse =
        NotificationCountsResponse(
            total = notificationRepository.countByUserId(user.id),
            unread = notificationRepository.countByUserIdAndReadAtIsNull(user.id),
        )

    @Transactional
    fun readNotifications(
        user: User,
        notificationIds: List<Long>,
    ) {
        notificationRepository.markAsReadByIds(user.id, notificationIds)
    }

    @Transactional
    fun readAllNotifications(user: User) {
        notificationRepository.markAllAsRead(user.id)
    }

    companion object {
        private val TRIGGERED_BY_TYPES =
            setOf(
                NotificationType.APPLICATION_RECEIVED,
                NotificationType.MEMBER_JOINED,
                NotificationType.MEMBER_LEFT,
            )

        private val HYDRATED_METADATA_KEYS = setOf("teamId", "postId", "triggeredBy")
    }
}
