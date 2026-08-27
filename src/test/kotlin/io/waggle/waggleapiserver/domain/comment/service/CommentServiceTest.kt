package io.waggle.waggleapiserver.domain.comment.service

import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.domain.comment.dto.request.CommentCreateRequest
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CommentServiceTest : CascadeIntegrationTestSupport() {
    private fun getComments(postId: Long) = commentService.getComments(postId, CursorGetQuery(cursor = null))

    @Test
    fun `답글에 답글을 달면 1-depth 제한에 걸린다`() {
        val author = createUser("author")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)
        val root = createComment(post.id, author.id)
        val reply = createComment(post.id, author.id, parentId = root.id)

        assertThatThrownBy {
            commentService.createComment(
                post.id,
                CommentCreateRequest(parentId = reply.id, content = "답글의 답글"),
                author,
            )
        }.isInstanceOf(BusinessException::class.java)
    }

    @Test
    fun `답글이 있는 댓글을 삭제하면 tombstone으로 남고 본문과 작성자가 가려진다`() {
        val author = createUser("author")
        val replier = createUser("replier")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)
        val root = createComment(post.id, author.id, content = "원본 본문")
        createComment(post.id, replier.id, parentId = root.id)

        commentService.deleteComment(root.id, author)

        assertThat(
            count(
                "SELECT COUNT(*) FROM comments WHERE id = ? AND deleted_at IS NULL AND tombstoned_at IS NOT NULL",
                root.id,
            ),
        ).isEqualTo(1L)
        // 원문은 감사를 위해 DB에 보존
        assertThat(
            count("SELECT COUNT(*) FROM comments WHERE id = ? AND content = '원본 본문'", root.id),
        ).isEqualTo(1L)

        val data = getComments(post.id).data
        assertThat(data).hasSize(1)
        assertThat(data[0].tombstoned).isTrue()
        assertThat(data[0].content).isNull()
        assertThat(data[0].user).isNull()
        assertThat(data[0].replies).hasSize(1)
    }

    @Test
    fun `답글이 없는 댓글을 삭제하면 목록에서 사라진다`() {
        val author = createUser("author")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)
        val root = createComment(post.id, author.id)

        commentService.deleteComment(root.id, author)

        assertThat(
            count("SELECT COUNT(*) FROM comments WHERE id = ? AND deleted_at IS NULL", root.id),
        ).isZero()
        assertThat(getComments(post.id).data).isEmpty()
    }

    @Test
    fun `마지막 답글을 삭제하면 부모 tombstone까지 함께 정리된다`() {
        val author = createUser("author")
        val replier = createUser("replier")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)
        val root = createComment(post.id, author.id)
        val reply = createComment(post.id, replier.id, parentId = root.id)

        commentService.deleteComment(root.id, author)
        commentService.deleteComment(reply.id, replier)

        assertThat(count("SELECT COUNT(*) FROM comments WHERE deleted_at IS NULL")).isZero()
        assertThat(getComments(post.id).data).isEmpty()
    }

    @Test
    fun `commentCount는 목록에 보이는 항목 수와 일치한다`() {
        val author = createUser("author")
        val replier = createUser("replier")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)

        val tombstoned = createComment(post.id, author.id)
        createComment(post.id, replier.id, parentId = tombstoned.id)
        val standalone = createComment(post.id, author.id)
        createComment(post.id, author.id)

        commentService.deleteComment(tombstoned.id, author) // 답글이 있어 tombstone
        commentService.deleteComment(standalone.id, author) // 답글이 없어 soft delete

        // tombstone 1 + 답글 1 + 최상위 1 = 3. soft delete된 standalone만 빠짐
        assertThat(
            count("SELECT COUNT(*) FROM comments WHERE post_id = ? AND deleted_at IS NULL", post.id),
        ).isEqualTo(3L)
        assertThat(postService.getPost(post.id, author).commentCount).isEqualTo(3L)
        assertThat(getComments(post.id).data.sumOf { 1 + it.replies.size }).isEqualTo(3)
    }

    @Test
    fun `커서 페이지네이션은 최상위 기준으로 자르고 답글은 온전히 동봉한다`() {
        val author = createUser("author")
        val replier = createUser("replier")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)

        val roots = (1..3).map { createComment(post.id, author.id, content = "root$it") }
        roots.forEach { root ->
            repeat(2) { createComment(post.id, replier.id, parentId = root.id) }
        }

        val firstPage =
            commentService.getComments(
                post.id,
                CursorGetQuery(cursor = null, size = 2),
            )

        assertThat(firstPage.data).hasSize(2)
        assertThat(firstPage.hasNext).isTrue()
        assertThat(firstPage.nextCursor).isEqualTo(roots[1].id)
        // 잘린 페이지 안의 스레드는 답글이 온전히 포함됨
        assertThat(firstPage.data.map { it.replies.size }).containsExactly(2, 2)

        val secondPage =
            commentService.getComments(
                post.id,
                CursorGetQuery(cursor = firstPage.nextCursor, size = 2),
            )

        assertThat(secondPage.data).hasSize(1)
        assertThat(secondPage.hasNext).isFalse()
        assertThat(secondPage.data[0].id).isEqualTo(roots[2].id)
    }
}
