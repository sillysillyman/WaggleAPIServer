package io.waggle.waggleapiserver.domain.post.dto.response

import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.response.UserDetailResponse

data class PostDetailResponse(
    val postId: Long,
    val title: String,
    val content: String,
    val user: UserDetailResponse,
) {
    companion object {
        fun of(
            post: Post,
            user: User,
        ): PostDetailResponse =
            PostDetailResponse(
                post.id,
                post.title,
                post.content,
                UserDetailResponse.from(user),
            )
    }
}
