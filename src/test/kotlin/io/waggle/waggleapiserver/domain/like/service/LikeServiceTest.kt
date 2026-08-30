package io.waggle.waggleapiserver.domain.like.service

import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.like.LikeType
import io.waggle.waggleapiserver.domain.post.dto.request.PostGetQuery
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class LikeServiceTest : CascadeIntegrationTestSupport() {
    @Test
    fun `모집글 좋아요를 두 번 눌러도 한 번 누른 것과 같다`() {
        val author = createUser("author")
        val liker = createUser("liker")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)

        val first = likeService.like(LikeType.POST, post.id, liker)
        val second = likeService.like(LikeType.POST, post.id, liker)

        assertThat(first.liked).isTrue()
        assertThat(first.likeCount).isEqualTo(1L)
        assertThat(second.liked).isTrue()
        assertThat(second.likeCount).isEqualTo(1L)
        assertThat(count("SELECT COUNT(*) FROM likes WHERE type = 'POST' AND target_id = ?", post.id))
            .isEqualTo(1L)
    }

    @Test
    fun `좋아요 취소를 두 번 해도 없는 행 삭제는 무해하다`() {
        val author = createUser("author")
        val liker = createUser("liker")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)

        likeService.like(LikeType.POST, post.id, liker)

        val first = likeService.unlike(LikeType.POST, post.id, liker)
        val second = likeService.unlike(LikeType.POST, post.id, liker)

        assertThat(first.liked).isFalse()
        assertThat(first.likeCount).isZero()
        assertThat(second.liked).isFalse()
        assertThat(second.likeCount).isZero()
        assertThat(count("SELECT COUNT(*) FROM likes WHERE type = 'POST' AND target_id = ?", post.id))
            .isZero()
    }

    @Test
    fun `댓글 좋아요는 답글에도 독립적으로 달린다`() {
        val author = createUser("author")
        val liker = createUser("liker")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)
        val root = createComment(post.id, author.id)
        val reply = createComment(post.id, author.id, parentId = root.id)

        likeService.like(LikeType.COMMENT, root.id, liker)
        val replyResponse = likeService.like(LikeType.COMMENT, reply.id, author)

        assertThat(replyResponse.likeCount).isEqualTo(1L)
        assertThat(count("SELECT COUNT(*) FROM likes WHERE type = 'COMMENT'")).isEqualTo(2L)
    }

    @Test
    fun `없는 모집글이나 댓글에는 좋아요를 누를 수 없다`() {
        val liker = createUser("liker")

        assertThatThrownBy { likeService.like(LikeType.POST, 999L, liker) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENTITY_NOT_FOUND)
        assertThatThrownBy { likeService.like(LikeType.COMMENT, 999L, liker) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENTITY_NOT_FOUND)
    }

    @Test
    fun `soft delete된 모집글에는 좋아요를 누를 수 없다`() {
        val author = createUser("author")
        val liker = createUser("liker")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)

        postService.deletePost(post.id, author)

        assertThatThrownBy { likeService.like(LikeType.POST, post.id, liker) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENTITY_NOT_FOUND)
    }

    @Test
    fun `tombstone된 댓글에는 좋아요를 누를 수 없다`() {
        val author = createUser("author")
        val replier = createUser("replier")
        val liker = createUser("liker")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)
        val root = createComment(post.id, author.id)
        createComment(post.id, replier.id, parentId = root.id)

        commentService.deleteComment(root.id, author) // 답글이 있어 tombstone

        assertThatThrownBy { likeService.like(LikeType.COMMENT, root.id, liker) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENTITY_NOT_FOUND)
    }

    @Test
    fun `모집글 목록과 상세는 좋아요 수와 본인 좋아요 여부를 함께 준다`() {
        val author = createUser("author")
        val liker = createUser("liker")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)

        likeService.like(LikeType.POST, post.id, author)
        likeService.like(LikeType.POST, post.id, liker)

        val detail = postService.getPost(post.id, liker)
        assertThat(detail.likeCount).isEqualTo(2L)
        assertThat(detail.liked).isTrue()

        val listed = postService.getPosts(PostGetQuery(), CursorGetQuery(cursor = null), liker).data
        assertThat(listed).hasSize(1)
        assertThat(listed[0].likeCount).isEqualTo(2L)
        assertThat(listed[0].liked).isTrue()
    }

    @Test
    fun `비로그인 조회는 좋아요 수만 보이고 liked는 false다`() {
        val author = createUser("author")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)

        likeService.like(LikeType.POST, post.id, author)

        val detail = postService.getPost(post.id, null)
        assertThat(detail.likeCount).isEqualTo(1L)
        assertThat(detail.liked).isFalse()

        val listed = postService.getPosts(PostGetQuery(), CursorGetQuery(cursor = null), null).data
        assertThat(listed[0].likeCount).isEqualTo(1L)
        assertThat(listed[0].liked).isFalse()
    }

    @Test
    fun `댓글 목록은 최상위 댓글과 답글 모두의 좋아요를 채운다`() {
        val author = createUser("author")
        val liker = createUser("liker")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)
        val root = createComment(post.id, author.id)
        val reply = createComment(post.id, author.id, parentId = root.id)

        likeService.like(LikeType.COMMENT, root.id, liker)
        likeService.like(LikeType.COMMENT, root.id, author)
        likeService.like(LikeType.COMMENT, reply.id, liker)

        val data = commentService.getComments(post.id, CursorGetQuery(cursor = null), liker).data

        assertThat(data).hasSize(1)
        assertThat(data[0].likeCount).isEqualTo(2L)
        assertThat(data[0].liked).isTrue()
        assertThat(data[0].replies[0].likeCount).isEqualTo(1L)
        assertThat(data[0].replies[0].liked).isTrue()
    }
}
