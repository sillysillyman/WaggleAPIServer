package io.waggle.waggleapiserver.domain.notification.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.notification.Notification
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.notification.dto.request.NotificationCreateRequest
import io.waggle.waggleapiserver.domain.notification.dto.response.NotificationResponse
import io.waggle.waggleapiserver.domain.notification.repository.NotificationRepository
import io.waggle.waggleapiserver.domain.team.dto.response.TeamResponse
import io.waggle.waggleapiserver.domain.team.repository.TeamRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class NotificationService(
    private val memberRepository: MemberRepository,
    private val notificationRepository: NotificationRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun createNotification(request: NotificationCreateRequest) {
        val (type, teamId, userId) = request

        if (!userRepository.existsById(userId)) {
            throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "User not found: $userId")
        }

        if (teamId != null && !teamRepository.existsById(teamId)) {
            throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Team not found: $teamId")
        }

        val notification =
            Notification(
                type = type,
                teamId = teamId,
                userId = userId,
            )

        notificationRepository.save(notification)
    }

    fun getUserNotifications(user: User): List<NotificationResponse> {
        val notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.id)

        val teamIds = notifications.mapNotNull { it.teamId }.distinct()
        val teamById = teamRepository.findAllById(teamIds).associateBy { it.id }
        val memberCountByTeamId = memberRepository.countByTeamIds(teamIds)
            .associate { it.teamId to it.count.toInt() }

        val triggeredByUserIds = notifications.mapNotNull { it.triggeredBy }
        val triggeredByUserById = userRepository.findAllById(triggeredByUserIds).associateBy { it.id }

        return notifications.map { notification ->
            val team = notification.teamId?.let { teamById[it] }?.let {
                TeamResponse.of(it, memberCountByTeamId[it.id] ?: 0)
            }

            val triggeredBy = notification.triggeredBy?.let {
                triggeredByUserById[it]?.let { user -> NotificationResponse.TriggeredByResponse.of(user) }
            }

            NotificationResponse.of(
                notification = notification,
                team = team,
                triggeredBy = triggeredBy,
            )
        }
    }
}
