package io.waggle.waggleapiserver.domain.post.service

import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.post.dto.request.PostSearchQuery
import io.waggle.waggleapiserver.domain.post.dto.request.PostUpsertRequest
import io.waggle.waggleapiserver.domain.post.dto.response.PostDetailResponse
import io.waggle.waggleapiserver.domain.post.dto.response.PostSimpleResponse
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.project.repository.ProjectRepository
import io.waggle.waggleapiserver.domain.user.User
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostService(
    private val memberRepository: MemberRepository,
    private val postRepository: PostRepository,
    private val projectRepository: ProjectRepository,
) {
    @Transactional
    fun createPost(
        request: PostUpsertRequest,
        user: User,
    ) {
        val (projectId, title, content) = request
        val project =
            if (projectId != null) {
                val member =
                    memberRepository.findByUserIdAndProjectId(user.id, projectId)
                        ?: throw EntityNotFoundException("Member not found: $user.id, $projectId")
                member.validateCanCreatePost()

                projectRepository.getReferenceById(projectId)
            } else {
                null
            }

        val post = Post(title = title, content = content, user = user, project = project)
        postRepository.save(post)
    }

    fun getPosts(
        query: PostSearchQuery,
        pageable: Pageable,
    ): Page<PostSimpleResponse> {
        val posts = postRepository.findWithFilter(query.query, pageable)
        return posts.map { post -> PostSimpleResponse.of(post, post.user) }
    }

    fun getPost(postId: Long): PostDetailResponse {
        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw EntityNotFoundException("Post not found: $postId")
        return PostDetailResponse.from(post)
    }

    @Transactional
    fun updatePost(
        postId: Long,
        request: PostUpsertRequest,
        user: User,
    ): PostDetailResponse {
        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw EntityNotFoundException("Post not found: $postId")
        post.validateOwnership(user.id)

        val (projectId, title, content) = request
        post.update(title, content)

        return PostDetailResponse.from(post)
    }

    @Transactional
    fun deletePost(
        postId: Long,
        user: User,
    ) {
        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw EntityNotFoundException("Post not found: $postId")

        post.validateOwnership(user.id)

        postRepository.delete(post)
    }
}
