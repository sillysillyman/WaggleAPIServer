package io.waggle.waggleapiserver.domain.application.service

import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.dto.response.CursorResponse
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.application.Application
import io.waggle.waggleapiserver.domain.application.ApplicationRead
import io.waggle.waggleapiserver.domain.application.ApplicationStatus
import io.waggle.waggleapiserver.domain.application.dto.request.ApplicationCreateRequest
import io.waggle.waggleapiserver.domain.application.dto.request.ApplicationUpdateRequest
import io.waggle.waggleapiserver.domain.application.dto.response.ApplicationResponse
import io.waggle.waggleapiserver.domain.application.dto.response.TeamApplicationResponse
import io.waggle.waggleapiserver.domain.application.dto.response.UserApplicationCountsResponse
import io.waggle.waggleapiserver.domain.application.dto.response.UserApplicationResponse
import io.waggle.waggleapiserver.domain.application.repository.ApplicationReadRepository
import io.waggle.waggleapiserver.domain.application.repository.ApplicationRepository
import io.waggle.waggleapiserver.domain.member.Member
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.notification.event.ApplicationReceivedEvent
import io.waggle.waggleapiserver.domain.notification.event.ApplicationRejectedEvent
import io.waggle.waggleapiserver.domain.notification.event.MemberJoinedEvent
import io.waggle.waggleapiserver.domain.notification.event.TeamJoinedEvent
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.recruitment.repository.RecruitmentRepository
import io.waggle.waggleapiserver.domain.team.repository.TeamRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ApplicationService(
    private val eventPublisher: ApplicationEventPublisher,
    private val applicationRepository: ApplicationRepository,
    private val applicationReadRepository: ApplicationReadRepository,
    private val memberRepository: MemberRepository,
    private val postRepository: PostRepository,
    private val recruitmentRepository: RecruitmentRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun applyToTeam(
        teamId: Long,
        request: ApplicationCreateRequest,
        user: User,
    ): ApplicationResponse {
        val (postId, position, detail, portfolioUrls) = request

        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Post not found: $postId")

        if (post.teamId != teamId) {
            throw BusinessException(
                ErrorCode.INVALID_STATE,
                "Post $postId does not belong to team $teamId",
            )
        }

        if (memberRepository.existsByUserIdAndTeamId(user.id, teamId)) {
            throw BusinessException(
                ErrorCode.DUPLICATE_RESOURCE,
                "Already a member of team: $teamId",
            )
        }

        val recruitment =
            recruitmentRepository.findForUpdateByPostIdAndPosition(postId, position)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Recruitment not found: $postId, $position",
                )

        if (!recruitment.isRecruiting()) {
            throw BusinessException(ErrorCode.INVALID_STATE, "$position is no longer recruiting")
        }

        if (applicationRepository.existsByPostIdAndUserIdAndPosition(
                postId,
                user.id,
                position,
            )
        ) {
            throw BusinessException(
                ErrorCode.DUPLICATE_RESOURCE,
                "Already applied to post: $postId, position: $position",
            )
        }

        val application =
            Application(
                position = position,
                teamId = teamId,
                postId = postId,
                userId = user.id,
                detail = detail,
            )
        application.portfolioUrls.addAll(portfolioUrls)
        val savedApplication = applicationRepository.save(application)

        eventPublisher.publishEvent(
            ApplicationReceivedEvent(
                teamId = teamId,
                postId = post.id,
                position = savedApplication.position,
                triggeredBy = user.id,
            ),
        )

        return ApplicationResponse.from(savedApplication)
    }

    @Transactional
    fun markApplicationAsRead(
        applicationId: Long,
        user: User,
    ): TeamApplicationResponse {
        val application =
            applicationRepository.findByIdOrNull(applicationId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Application not found: $applicationId",
                )

        val member =
            memberRepository.findByUserIdAndTeamId(user.id, application.teamId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found")
        member.checkMemberRole(MemberRole.MANAGER)

        if (!applicationReadRepository.existsByApplicationIdAndUserId(applicationId, user.id)) {
            val applicationRead =
                ApplicationRead(
                    applicationId = applicationId,
                    userId = user.id,
                )
            applicationReadRepository.save(applicationRead)
        }

        val applicant =
            userRepository.findByIdOrNull(application.userId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "User not found: ${application.userId}",
                )

        return TeamApplicationResponse.of(application, applicant, isRead = true)
    }

    fun getUserApplications(
        status: ApplicationStatus?,
        cursorQuery: CursorGetQuery,
        user: User,
    ): CursorResponse<UserApplicationResponse> {
        val pageable = PageRequest.of(0, cursorQuery.size + 1)
        val applications =
            applicationRepository.findByUserIdWithCursor(
                userId = user.id,
                status = status,
                cursor = cursorQuery.cursor,
                pageable = pageable,
            )

        val hasNext = applications.size > cursorQuery.size
        val slicedApplications = if (hasNext) applications.take(cursorQuery.size) else applications

        val teamIds = slicedApplications.map { it.teamId }.distinct()
        val postIds = slicedApplications.map { it.postId }.distinct()
        val teamById = teamRepository.findAllById(teamIds).associateBy { it.id }
        val postById = postRepository.findAllById(postIds).associateBy { it.id }

        val data =
            slicedApplications.map { application ->
                val team =
                    teamById[application.teamId]
                        ?: throw BusinessException(
                            ErrorCode.ENTITY_NOT_FOUND,
                            "Team not found: ${application.teamId}",
                        )
                val post =
                    postById[application.postId]
                        ?: throw BusinessException(
                            ErrorCode.ENTITY_NOT_FOUND,
                            "Post not found: ${application.postId}",
                        )
                UserApplicationResponse.of(application, team, post)
            }

        return CursorResponse(
            data = data,
            nextCursor = if (hasNext) slicedApplications.lastOrNull()?.id else null,
            hasNext = hasNext,
        )
    }

    fun getUserApplicationCounts(user: User): UserApplicationCountsResponse {
        val countByStatus =
            applicationRepository
                .countByUserIdGroupByStatus(user.id)
                .associate { it.status to it.count }
        val pending = countByStatus[ApplicationStatus.PENDING] ?: 0L
        val approved = countByStatus[ApplicationStatus.APPROVED] ?: 0L
        val rejected = countByStatus[ApplicationStatus.REJECTED] ?: 0L
        return UserApplicationCountsResponse(
            total = pending + approved + rejected,
            pending = pending,
            approved = approved,
            rejected = rejected,
        )
    }

    fun getTeamApplications(
        teamId: Long,
        postId: Long?,
        cursorQuery: CursorGetQuery,
        user: User,
    ): CursorResponse<TeamApplicationResponse> {
        val member =
            memberRepository.findByUserIdAndTeamId(user.id, teamId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found")
        member.checkMemberRole(MemberRole.MANAGER)

        val pageable = PageRequest.of(0, cursorQuery.size + 1)
        val cursorStatusPriority =
            cursorQuery.cursor?.let { cursorId ->
                applicationRepository.findByIdOrNull(cursorId)?.statusPriority
                    ?: throw BusinessException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "Application not found: $cursorId",
                    )
            }
        val applications =
            if (postId != null) {
                val post =
                    postRepository.findByIdOrNull(postId)
                        ?: throw BusinessException(
                            ErrorCode.ENTITY_NOT_FOUND,
                            "Post not found: $postId",
                        )
                if (post.teamId != teamId) {
                    throw BusinessException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "Post $postId does not belong to team $teamId",
                    )
                }
                applicationRepository.findByPostIdWithCursor(
                    postId,
                    cursorQuery.cursor,
                    cursorStatusPriority,
                    pageable,
                )
            } else {
                applicationRepository.findByTeamIdWithCursor(
                    teamId,
                    cursorQuery.cursor,
                    cursorStatusPriority,
                    pageable,
                )
            }

        val hasNext = applications.size > cursorQuery.size
        val slicedApplications = if (hasNext) applications.take(cursorQuery.size) else applications

        val applicantIds = slicedApplications.map { it.userId }.distinct()
        val applicantById = userRepository.findAllById(applicantIds).associateBy { it.id }

        val readApplicationIdSet =
            applicationReadRepository
                .findReadApplicationIds(user.id, slicedApplications.map { it.id })
                .toSet()

        val data =
            slicedApplications.map { application ->
                val applicant =
                    applicantById[application.userId]
                        ?: throw BusinessException(
                            ErrorCode.ENTITY_NOT_FOUND,
                            "User not found: ${application.userId}",
                        )
                TeamApplicationResponse.of(
                    application,
                    applicant,
                    isRead = readApplicationIdSet.contains(application.id),
                )
            }

        return CursorResponse(
            data = data,
            nextCursor = if (hasNext) slicedApplications.lastOrNull()?.id else null,
            hasNext = hasNext,
        )
    }

    @Transactional
    fun updateApplicationStatus(
        applicationId: Long,
        status: ApplicationStatus,
        user: User,
    ) {
        when (status) {
            ApplicationStatus.APPROVED -> approveApplication(applicationId, user)

            ApplicationStatus.REJECTED -> rejectApplication(applicationId, user)

            else -> throw BusinessException(
                ErrorCode.INVALID_INPUT_VALUE,
                "Cannot change status to $status",
            )
        }
    }

    @Transactional
    fun updateApplication(
        applicationId: Long,
        request: ApplicationUpdateRequest,
        user: User,
    ): ApplicationResponse {
        val (detail, portfolioUrls) = request

        val application =
            applicationRepository.findByIdAndUserId(applicationId, user.id)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Application not found: $applicationId",
                )

        application.update(detail, portfolioUrls)

        return ApplicationResponse.from(application)
    }

    @Transactional
    fun deleteApplication(
        applicationId: Long,
        user: User,
    ) {
        val application =
            applicationRepository.findByIdAndUserId(applicationId, user.id)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Application not found: $applicationId",
                )

        if (application.status != ApplicationStatus.PENDING) {
            throw BusinessException(
                ErrorCode.INVALID_STATE,
                "Only PENDING applications can be cancelled",
            )
        }

        application.delete()
    }

    private fun approveApplication(
        applicationId: Long,
        user: User,
    ) {
        val application =
            applicationRepository.findByIdOrNull(applicationId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Application not found: $applicationId",
                )

        val approver =
            memberRepository.findByUserIdAndTeamId(user.id, application.teamId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found")
        approver.checkMemberRole(MemberRole.MANAGER)

        application.updateStatus(ApplicationStatus.APPROVED)

        val existingMember =
            memberRepository.findByUserIdAndTeamIdIncludingDeleted(
                application.userId,
                application.teamId,
            )

        when {
            existingMember == null ->
                memberRepository.save(
                    Member(
                        userId = application.userId,
                        teamId = application.teamId,
                        position = application.position,
                        role = MemberRole.MEMBER,
                        admittedBy = user.id,
                    ),
                )

            existingMember.deletedAt != null ->
                existingMember.reactivate(
                    position = application.position,
                    role = MemberRole.MEMBER,
                    admittedBy = user.id,
                )

            else ->
                throw BusinessException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "User is already a member of team: ${application.teamId}",
                )
        }

        applicationRepository.updateDeletedAtByUserIdAndTeamIdAndIdNotAndStatusPendingAndDeletedAtIsNull(
            userId = application.userId,
            teamId = application.teamId,
            excludedId = application.id,
        )

        eventPublisher.publishEvent(
            TeamJoinedEvent(
                teamId = application.teamId,
                joinedUserId = application.userId,
                triggeredBy = user.id,
            ),
        )
        eventPublisher.publishEvent(
            MemberJoinedEvent(
                teamId = application.teamId,
                triggeredBy = application.userId,
            ),
        )
    }

    private fun rejectApplication(
        applicationId: Long,
        user: User,
    ) {
        val application =
            applicationRepository.findByIdOrNull(applicationId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Application not found: $applicationId",
                )

        val member =
            memberRepository.findByUserIdAndTeamId(user.id, application.teamId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found")
        member.checkMemberRole(MemberRole.MANAGER)

        application.updateStatus(ApplicationStatus.REJECTED)

        eventPublisher.publishEvent(
            ApplicationRejectedEvent(
                teamId = application.teamId,
                applicantUserId = application.userId,
                triggeredBy = user.id,
            ),
        )
    }
}
