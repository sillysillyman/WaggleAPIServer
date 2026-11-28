package io.waggle.waggleapiserver.domain.post.service

import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.dto.response.CursorResponse
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.common.storage.ImageDirectory
import io.waggle.waggleapiserver.common.storage.StorageClient
import io.waggle.waggleapiserver.common.storage.dto.request.PresignedUrlRequest
import io.waggle.waggleapiserver.common.storage.dto.response.PresignedUrlResponse
import io.waggle.waggleapiserver.domain.application.repository.ApplicationRepository
import io.waggle.waggleapiserver.domain.comment.repository.CommentRepository
import io.waggle.waggleapiserver.domain.like.LikeId
import io.waggle.waggleapiserver.domain.like.LikeType
import io.waggle.waggleapiserver.domain.like.repository.LikeRepository
import io.waggle.waggleapiserver.domain.member.Member
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.post.Deadline
import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.post.dto.request.PostCreateRequest
import io.waggle.waggleapiserver.domain.post.dto.request.PostGetQuery
import io.waggle.waggleapiserver.domain.post.dto.request.PostUpdateRequest
import io.waggle.waggleapiserver.domain.post.dto.response.PostDetailResponse
import io.waggle.waggleapiserver.domain.post.dto.response.PostSimpleResponse
import io.waggle.waggleapiserver.domain.post.dto.response.TeamPostSimpleResponse
import io.waggle.waggleapiserver.domain.post.event.PostDeletedEvent
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.recruitment.Recruitment
import io.waggle.waggleapiserver.domain.recruitment.RecruitmentStatus
import io.waggle.waggleapiserver.domain.recruitment.dto.request.RecruitmentUpdateStatusRequest
import io.waggle.waggleapiserver.domain.recruitment.dto.response.RecruitmentResponse
import io.waggle.waggleapiserver.domain.recruitment.repository.RecruitmentRepository
import io.waggle.waggleapiserver.domain.team.dto.response.TeamResponse
import io.waggle.waggleapiserver.domain.team.repository.TeamRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostService(
    private val eventPublisher: ApplicationEventPublisher,
    private val storageClient: StorageClient,
    private val postViewService: PostViewService,
    private val applicationRepository: ApplicationRepository,
    private val commentRepository: CommentRepository,
    private val likeRepository: LikeRepository,
    private val memberRepository: MemberRepository,
    private val postRepository: PostRepository,
    private val recruitmentRepository: RecruitmentRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun createPost(
        request: PostCreateRequest,
        user: User,
    ): PostDetailResponse {
        val (teamId, title, content, recruitments, deadline) = request

        // 애너테이션으로 검사하면 서버가 UTC라 KST 새벽에 어제 날짜가 통과함
        val expiresAt = deadline?.let { Deadline.toExpiresAt(it) }
        if (expiresAt != null && Deadline.isPast(expiresAt)) {
            throw BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Deadline cannot be in the past")
        }

        val team =
            teamRepository.findByIdOrNull(teamId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Team not found: $teamId")
        if (team.isCompleted) {
            throw BusinessException(
                ErrorCode.INVALID_STATE,
                "Cannot create post for completed team: $teamId",
            )
        }

        val member =
            memberRepository.findByUserIdAndTeamId(user.id, teamId)
                ?: throw BusinessException(ErrorCode.ACCESS_DENIED, "Not a member of team: $teamId")
        member.checkMemberRole(MemberRole.MANAGER)

        val post =
            Post(
                title = title,
                content = content,
                userId = user.id,
                teamId = teamId,
                expiresAt = expiresAt,
            )
        val savedPost = postRepository.save(post)

        val savedRecruitments =
            recruitmentRepository.saveAll(
                recruitments.map {
                    Recruitment(
                        position = it.position,
                        count = it.count,
                        postId = savedPost.id,
                        skills = it.skills.toMutableSet(),
                    )
                },
            )

        val memberCount = memberRepository.countByTeamId(teamId)

        return PostDetailResponse.of(
            savedPost,
            UserSimpleResponse.from(user),
            TeamResponse.of(team, memberCount, member.role),
            savedRecruitments.map { RecruitmentResponse.from(it) },
        )
    }

    fun generateContentImagePresignedUrl(
        request: PresignedUrlRequest,
        user: User,
    ): PresignedUrlResponse {
        val presignedUploadUrl =
            storageClient.generateUploadUrl(
                ImageDirectory.POST_CONTENT,
                user.id,
                request.contentType,
            )
        return PresignedUrlResponse.from(presignedUploadUrl)
    }

    fun getPosts(
        query: PostGetQuery,
        cursorQuery: CursorGetQuery,
        user: User?,
    ): CursorResponse<PostSimpleResponse> {
        val posts =
            postRepository.findWithFilter(
                cursor = cursorQuery.cursor,
                q = query.q,
                positions = query.positions ?: emptySet(),
                skills = query.skills ?: emptySet(),
                sort = query.sort,
                size = cursorQuery.size + 1,
            )

        val hasNext = posts.size > cursorQuery.size
        val content = if (hasNext) posts.take(cursorQuery.size) else posts
        val nextCursor = if (hasNext) content.last().id else null

        val authorIds = content.map { it.userId }.distinct()
        val authorById = userRepository.findAllById(authorIds).associateBy { it.id }

        val postIds = content.map { it.id }
        val recruitmentsByPostId =
            recruitmentRepository.findByPostIdIn(postIds).groupBy { it.postId }

        val commentCountByPostId =
            if (postIds.isEmpty()) {
                emptyMap()
            } else {
                commentRepository
                    .countCommentsGroupByPostId(postIds)
                    .associate { it.postId to it.commentCount }
            }
        val pendingViewCountByPostId = postViewService.getPendingViewCountByPostId(postIds)

        val likedPostIdSet =
            if (user == null || postIds.isEmpty()) {
                emptySet()
            } else {
                likeRepository
                    .findTargetIdsByUserIdAndTypeAndTargetIdIn(user.id, LikeType.POST, postIds)
                    .toSet()
            }

        val data =
            content.map { post ->
                val author =
                    authorById[post.userId]
                        ?: throw BusinessException(
                            ErrorCode.ENTITY_NOT_FOUND,
                            "User not found: ${post.userId}",
                        )
                val recruitments =
                    (
                        recruitmentsByPostId[post.id]
                            ?: emptyList()
                    ).map { RecruitmentResponse.from(it) }
                PostSimpleResponse.of(
                    post,
                    UserSimpleResponse.from(author),
                    recruitments,
                    commentCountByPostId[post.id] ?: 0,
                    post.viewCount + (pendingViewCountByPostId[post.id] ?: 0),
                    post.likeCount,
                    post.id in likedPostIdSet,
                )
            }

        return CursorResponse(
            data = data,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    fun getPost(
        postId: Long,
        user: User?,
    ): PostDetailResponse {
        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Post not found: $postId")
        val author =
            userRepository.findByIdOrNull(post.userId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "User not found: ${post.userId}",
                )
        val team =
            teamRepository.findByIdOrNull(post.teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Team not found: ${post.teamId}",
                )
        val recruitments =
            recruitmentRepository.findByPostId(postId).map { RecruitmentResponse.from(it) }

        val pendingViewCount = postViewService.incrementViewCount(postId)

        val memberCount = memberRepository.countByTeamId(team.id)

        val memberRole =
            user?.let { memberRepository.findByUserIdAndTeamId(it.id, post.teamId)?.role }
        val applicationStatus =
            user?.let { applicationRepository.findByPostIdAndUserId(postId, it.id)?.status }
        val formerMember =
            user != null &&
                memberRole == null &&
                memberRepository.findByUserIdAndTeamIdAndDeletedAtIsNotNull(user.id, post.teamId) != null

        return PostDetailResponse.of(
            post,
            UserSimpleResponse.from(author),
            TeamResponse.of(team, memberCount, memberRole),
            recruitments,
            commentRepository.countByPostId(postId),
            post.viewCount + pendingViewCount,
            post.likeCount,
            user?.let { likeRepository.existsById(LikeId(LikeType.POST, postId, it.id)) } ?: false,
            applicationStatus,
            formerMember,
        )
    }

    fun getTeamPosts(
        teamId: Long,
        user: User?,
    ): List<TeamPostSimpleResponse> {
        val posts = postRepository.findByTeamIdOrderByIdDesc(teamId)

        val authorIds = posts.map { it.userId }.distinct()
        val authorById = userRepository.findAllById(authorIds).associateBy { it.id }

        val postIds = posts.map { it.id }
        val recruitmentsByPostId =
            recruitmentRepository.findByPostIdIn(postIds).groupBy { it.postId }

        val commentCountByPostId =
            if (postIds.isEmpty()) {
                emptyMap()
            } else {
                commentRepository
                    .countCommentsGroupByPostId(postIds)
                    .associate { it.postId to it.commentCount }
            }
        val pendingViewCountByPostId = postViewService.getPendingViewCountByPostId(postIds)

        val isMember =
            user?.let { memberRepository.existsByUserIdAndTeamId(it.id, teamId) } ?: false

        val applicantCountByPostId =
            if (isMember && postIds.isNotEmpty()) {
                applicationRepository
                    .countApplicantsGroupByPostId(postIds)
                    .associate { it.postId to it.applicantCount.toInt() }
            } else {
                emptyMap()
            }

        val unreadApplicationCountByPostId =
            if (isMember && postIds.isNotEmpty()) {
                applicationRepository
                    .countUnreadApplicationsGroupByPostId(user.id, postIds)
                    .associate { it.postId to it.unreadCount.toInt() }
            } else {
                emptyMap()
            }

        return posts.map { post ->
            val author =
                authorById[post.userId]
                    ?: throw BusinessException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "User not found: ${post.userId}",
                    )
            val recruitments =
                (recruitmentsByPostId[post.id] ?: emptyList()).map { RecruitmentResponse.from(it) }
            val (applicantCount, unreadApplicationCount) =
                if (isMember) {
                    (
                        applicantCountByPostId[post.id]
                            ?: 0
                    ) to (unreadApplicationCountByPostId[post.id] ?: 0)
                } else {
                    null to null
                }
            TeamPostSimpleResponse.of(
                post,
                UserSimpleResponse.from(author),
                recruitments,
                commentCountByPostId[post.id] ?: 0,
                post.viewCount + (pendingViewCountByPostId[post.id] ?: 0),
                applicantCount,
                unreadApplicationCount,
            )
        }
    }

    @Transactional
    fun updatePost(
        postId: Long,
        request: PostUpdateRequest,
        user: User,
    ): PostDetailResponse {
        val (title, content, recruitments, deadline) = request

        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Post not found: $postId")

        val member = getMemberManagingPost(post, user)
        post.update(title, content, deadline?.let { Deadline.toExpiresAt(it) })

        // 잠그지 않으면 진행 중인 승인보다 옛 approvedCount로 검사해 승인 인원보다 적게 줄일 수 있음
        val existingRecruitmentByPosition =
            recruitmentRepository.findForUpdateByPostId(postId).associateBy { it.position }
        val requestedRecruitmentByPosition = recruitments.associateBy { it.position }

        val recruitmentsToDelete =
            existingRecruitmentByPosition.filterKeys { it !in requestedRecruitmentByPosition }.values
        recruitmentsToDelete.forEach { checkDeletableRecruitment(it) }
        recruitmentRepository.deleteAll(recruitmentsToDelete)

        var closedByUpdate = false
        val updatedRecruitments =
            requestedRecruitmentByPosition.mapNotNull { (position, requestedRecruitment) ->
                existingRecruitmentByPosition[position]?.also {
                    val wasRecruiting = it.isRecruiting()
                    it.update(
                        requestedRecruitment.count,
                        requestedRecruitment.skills,
                    )
                    it.closeIfFull(it.approvedCount)
                    closedByUpdate = closedByUpdate || (wasRecruiting && !it.isRecruiting())
                }
            }

        val newRecruitments = requestedRecruitmentByPosition.filterKeys { it !in existingRecruitmentByPosition }
        // 새 포지션은 RECRUITING으로 들어가므로 마감된 글이 되살아남
        // 같은 요청에서 기한을 연장했다면 통과함
        if (newRecruitments.isNotEmpty()) {
            post.checkNotExpired()
        }

        val insertedRecruitments =
            recruitmentRepository.saveAll(
                newRecruitments
                    .values
                    .map {
                        Recruitment(
                            position = it.position,
                            count = it.count,
                            postId = postId,
                            skills = it.skills.toMutableSet(),
                        )
                    },
            )

        val savedRecruitments = updatedRecruitments + insertedRecruitments
        // 수동 마감처럼 마감일 표시를 실제로 마감된 날로 맞춤
        if (closedByUpdate && savedRecruitments.none { it.isRecruiting() }) {
            post.limitDeadlineToToday()
        }

        val team =
            teamRepository.findByIdOrNull(post.teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Team not found: ${post.teamId}",
                )
        val memberCount = memberRepository.countByTeamId(post.teamId)

        return PostDetailResponse.of(
            post,
            UserSimpleResponse.from(user),
            TeamResponse.of(team, memberCount, member.role),
            savedRecruitments.map { RecruitmentResponse.from(it) },
            commentCount = commentRepository.countByPostId(postId),
            viewCount = post.viewCount + postViewService.getPendingViewCount(postId),
            likeCount = post.likeCount,
            liked = likeRepository.existsById(LikeId(LikeType.POST, postId, user.id)),
        )
    }

    // 멤버 확인이 작성자 확인보다 먼저여야 팀을 떠난 작성자가 막힘
    private fun getMemberManagingPost(
        post: Post,
        user: User,
    ): Member {
        val member =
            memberRepository.findByUserIdAndTeamId(user.id, post.teamId)
                ?: throw BusinessException(ErrorCode.ACCESS_DENIED, "Not a member of team: ${post.teamId}")
        if (post.userId != user.id) {
            member.checkMemberRole(MemberRole.MANAGER)
        }
        return member
    }

    // 마감한 포지션을 지웠다 다시 추가하면 RECRUITING으로 되살아나 재개 금지가 우회됨
    // 지원서는 recruitment가 아니라 position을 참조해 삭제해도 남으므로 대응 모집 정보가 없는 지원서가 생김
    private fun checkDeletableRecruitment(recruitment: Recruitment) {
        if (!recruitment.isRecruiting()) {
            throw BusinessException(
                ErrorCode.INVALID_STATE,
                "Cannot delete a closed recruitment: ${recruitment.position}",
            )
        }
        if (applicationRepository.existsByPostIdAndPosition(recruitment.postId, recruitment.position)) {
            throw BusinessException(
                ErrorCode.INVALID_STATE,
                "Cannot delete a recruitment with applications: ${recruitment.position}",
            )
        }
    }

    @Transactional
    fun updatePostRecruitmentStatus(
        postId: Long,
        request: RecruitmentUpdateStatusRequest,
        user: User,
    ) {
        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Post not found: $postId")

        val member =
            memberRepository.findByUserIdAndTeamId(user.id, post.teamId)
                ?: throw BusinessException(ErrorCode.ACCESS_DENIED, "Not a member of team: ${post.teamId}")
        member.checkMemberRole(MemberRole.MANAGER)

        // 현재 기획상 마감 상태를 재모집으로 변경하지 않음
        if (request.status != RecruitmentStatus.CLOSED) {
            throw BusinessException(ErrorCode.INVALID_STATE, "Recruitment cannot be reopened")
        }

        // 열린 포지션이 하나도 없으면 이미 마감된 글이라, 중복 마감을 걸러내려면 먼저 추려야 함
        val openRecruitments = recruitmentRepository.findByPostId(postId).filter { it.isRecruiting() }
        if (openRecruitments.isEmpty()) {
            throw BusinessException(ErrorCode.INVALID_STATE, "Post is already closed: $postId")
        }
        openRecruitments.forEach { it.close() }
        post.limitDeadlineToToday()
    }

    @Transactional
    fun deletePost(
        postId: Long,
        user: User,
    ) {
        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Post not found: $postId")
        getMemberManagingPost(post, user)

        eventPublisher.publishEvent(PostDeletedEvent(postId))

        post.delete()
    }
}
