package io.waggle.waggleapiserver.domain.post.service

import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.post.dto.request.PostSearchQuery
import io.waggle.waggleapiserver.domain.post.dto.request.PostUpsertRequest
import io.waggle.waggleapiserver.domain.post.dto.response.PostDetailResponse
import io.waggle.waggleapiserver.domain.post.dto.response.PostSimpleResponse
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.project.repository.ProjectRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostService(
    private val memberRepository: MemberRepository,
    private val postRepository: PostRepository,
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun createPost(
        request: PostUpsertRequest,
        user: User,
    ): PostDetailResponse {
        val (projectId, title, content) = request

        if (projectId != null) {
            val member =
                memberRepository.findByUserIdAndProjectId(user.id, projectId)
                    ?: throw EntityNotFoundException("Member not found: ${user.id}, $projectId")
            member.checkMemberRole(MemberRole.MEMBER)
        }

        val post =
            Post(
                title = title,
                content = content,
                userId = user.id,
                projectId = projectId,
            )
        val savedPost = postRepository.save(post)

        return PostDetailResponse.of(savedPost, UserSimpleResponse.from(user))
    }

    fun getPosts(
        query: PostSearchQuery,
        pageable: Pageable,
    ): Page<PostSimpleResponse> {
        val posts = postRepository.findWithFilter(query.query, pageable)

        val userIds = posts.content.map { it.userId }.distinct()
        val userMap = userRepository.findAllById(userIds).associateBy { it.id }

        return posts.map { post ->
            val user =
                userMap[post.userId]
                    ?: throw EntityNotFoundException("User not found: ${post.userId}")
            PostSimpleResponse.of(post, UserSimpleResponse.from(user))
        }
    }

    fun getPost(postId: Long): PostDetailResponse {
        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw EntityNotFoundException("Post not found: $postId")
        val user =
            userRepository.findByIdOrNull(post.userId)
                ?: throw EntityNotFoundException("User not found: $post.userId")
        return PostDetailResponse.of(post, UserSimpleResponse.from(user))
    }

    @Transactional
    fun updatePost(
        postId: Long,
        request: PostUpsertRequest,
        user: User,
    ): PostDetailResponse {
        val (projectId, title, content) = request

        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw EntityNotFoundException("Post not found: $postId")
        post.checkOwnership(user.id)

        if (projectId != null) {
            val member =
                memberRepository.findByUserIdAndProjectId(user.id, projectId)
                    ?: throw EntityNotFoundException("Member not found: ${user.id}, $projectId")
            member.checkMemberRole(MemberRole.MEMBER)
        }

        post.update(title, content, projectId)

        return PostDetailResponse.of(post, UserSimpleResponse.from(user))
    }

    @Transactional
    fun deletePost(
        postId: Long,
        user: User,
    ) {
        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw EntityNotFoundException("Post not found: $postId")
        post.checkOwnership(user.id)

        post.delete()
    }
}
