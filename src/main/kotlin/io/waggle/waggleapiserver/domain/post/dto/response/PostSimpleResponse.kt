package io.waggle.waggleapiserver.domain.post.dto.response

import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkResponse
import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse

data class PostSimpleResponse(
    val postId: Long,
    val title: String,
    val content: String,
    val user: UserSimpleResponse,
) : BookmarkResponse {
    companion object {
        fun of(
            post: Post,
            user: User,
        ): PostSimpleResponse =
            PostSimpleResponse(
                post.id,
                post.title,
                post.content,
                UserSimpleResponse.from(user),
            )
    }
}
