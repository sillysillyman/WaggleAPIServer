package io.waggle.waggleapiserver.domain.post.service

import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.dto.response.CursorResponse
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.common.storage.StorageClient
import io.waggle.waggleapiserver.common.storage.dto.request.PresignedUrlRequest
import io.waggle.waggleapiserver.common.storage.dto.response.PresignedUrlResponse
import io.waggle.waggleapiserver.domain.application.repository.ApplicationRepository
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.repository.BookmarkRepository
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.post.PostSort
import io.waggle.waggleapiserver.domain.post.dto.request.PostCreateRequest
import io.waggle.waggleapiserver.domain.post.dto.request.PostGetQuery
import io.waggle.waggleapiserver.domain.post.dto.request.PostUpdateRequest
import io.waggle.waggleapiserver.domain.post.dto.response.PostDetailResponse
import io.waggle.waggleapiserver.domain.post.dto.response.PostSimpleResponse
import io.waggle.waggleapiserver.domain.post.dto.response.TeamPostSimpleResponse
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.recruitment.Recruitment
import io.waggle.waggleapiserver.domain.recruitment.dto.request.RecruitmentUpdateStatusRequest
import io.waggle.waggleapiserver.domain.recruitment.dto.response.RecruitmentResponse
import io.waggle.waggleapiserver.domain.recruitment.repository.RecruitmentRepository
import io.waggle.waggleapiserver.domain.team.dto.response.TeamResponse
import io.waggle.waggleapiserver.domain.team.repository.TeamRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostService(
    private val storageClient: StorageClient,
    private val applicationRepository: ApplicationRepository,
    private val bookmarkRepository: BookmarkRepository,
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
        val (teamId, title, content, recruitments) = request

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
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Member not found: ${user.id}, $teamId",
                )
        member.checkMemberRole(MemberRole.MANAGER)

        val post =
            Post(
                title = title,
                content = content,
                userId = user.id,
                teamId = teamId,
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

    fun generateContentImagePresignedUrl(request: PresignedUrlRequest): PresignedUrlResponse {
        val presignedUploadUrl =
            storageClient.generateUploadUrl(
                "posts",
                request.contentType,
            )
        return PresignedUrlResponse.from(presignedUploadUrl)
    }

    fun getPosts(
        query: PostGetQuery,
        cursorQuery: CursorGetQuery,
    ): CursorResponse<PostSimpleResponse> {
        val direction =
            when (query.sort) {
                PostSort.NEWEST -> Sort.Direction.DESC
                PostSort.OLDEST -> Sort.Direction.ASC
            }
        val posts =
            postRepository.findWithFilter(
                cursor = cursorQuery.cursor,
                q = query.q,
                positions = query.positions ?: emptySet(),
                skills = query.skills ?: emptySet(),
                sort = query.sort,
                pageable = PageRequest.of(0, cursorQuery.size + 1, Sort.by(direction, "id")),
            )

        val hasNext = posts.size > cursorQuery.size
        val content = if (hasNext) posts.take(cursorQuery.size) else posts
        val nextCursor = if (hasNext) content.last().id else null

        val authorIds = content.map { it.userId }.distinct()
        val authorById = userRepository.findAllById(authorIds).associateBy { it.id }

        val postIds = content.map { it.id }
        val recruitmentsByPostId =
            recruitmentRepository.findByPostIdIn(postIds).groupBy { it.postId }

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

        val memberCount = memberRepository.countByTeamId(team.id)

        val memberRole =
            user?.let { memberRepository.findByUserIdAndTeamId(it.id, post.teamId)?.role }
        val applicationStatus =
            user?.let { applicationRepository.findByPostIdAndUserId(postId, it.id)?.status }

        return PostDetailResponse.of(
            post,
            UserSimpleResponse.from(author),
            TeamResponse.of(team, memberCount, memberRole),
            recruitments,
            applicationStatus,
        )
    }

    fun getTeamPosts(
        teamId: Long,
        user: User?,
    ): List<TeamPostSimpleResponse> {
        val posts = postRepository.findByTeamIdOrderByCreatedAtDesc(teamId)

        val authorIds = posts.map { it.userId }.distinct()
        val authorById = userRepository.findAllById(authorIds).associateBy { it.id }

        val postIds = posts.map { it.id }
        val recruitmentsByPostId =
            recruitmentRepository.findByPostIdIn(postIds).groupBy { it.postId }

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
            if (isMember && postIds.isNotEmpty() && user != null) {
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
        val (title, content, recruitments) = request

        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Post not found: $postId")

        post.checkOwnership(user.id)
        post.update(title, content)

        val member =
            memberRepository.findByUserIdAndTeamId(user.id, post.teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Member not found: ${user.id}, ${post.teamId}",
                )

        val existingRecruitmentByPosition = recruitmentRepository.findByPostId(postId).associateBy { it.position }
        val requestedRecruitmentByPosition = recruitments.associateBy { it.position }

        val recruitmentsToDelete =
            existingRecruitmentByPosition.filterKeys { it !in requestedRecruitmentByPosition }.values
        recruitmentRepository.deleteAll(recruitmentsToDelete)

        val updatedRecruitments =
            requestedRecruitmentByPosition.mapNotNull { (position, requestedRecruitment) ->
                existingRecruitmentByPosition[position]?.also {
                    it.update(
                        requestedRecruitment.count,
                        requestedRecruitment.skills,
                    )
                }
            }

        val insertedRecruitments =
            recruitmentRepository.saveAll(
                requestedRecruitmentByPosition
                    .filterKeys { it !in existingRecruitmentByPosition }
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
        )
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
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Member not found: ${user.id}, ${post.teamId}",
                )
        member.checkMemberRole(MemberRole.MANAGER)

        val recruitments = recruitmentRepository.findByPostId(postId)
        recruitments.forEach { it.updateStatus(request.status) }
    }

    @Transactional
    fun deletePost(
        postId: Long,
        user: User,
    ) {
        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Post not found: $postId")
        post.checkOwnership(user.id)

        recruitmentRepository.deleteByPostId(postId)
        applicationRepository.updateDeletedAtByPostIdAndDeletedAtIsNull(postId)
        bookmarkRepository.deleteByIdTargetIdAndIdType(postId, BookmarkType.POST)

        post.delete()
    }
}
