package io.waggle.waggleapiserver.domain.post.dto.response

import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse

data class PostDetailResponse(
    val postId: Long,
    val title: String,
    val content: String,
    val user: UserSimpleResponse,
) {
    companion object {
        fun of(
            post: Post,
            userSimpleResponse: UserSimpleResponse,
        ): PostDetailResponse =
            PostDetailResponse(
                postId = post.id,
                title = post.title,
                content = post.content,
                user = userSimpleResponse,
            )
    }
}
