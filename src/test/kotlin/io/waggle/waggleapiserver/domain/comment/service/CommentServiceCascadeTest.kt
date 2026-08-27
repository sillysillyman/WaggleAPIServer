package io.waggle.waggleapiserver.domain.comment.service

import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CommentServiceCascadeTest : CascadeIntegrationTestSupport() {
    @Test
    fun `deletePost는 해당 글의 댓글과 답글을 모두 정리하고 형제 글은 보존한다`() {
        val owner = createUser("owner")
        val commenter = createUser("commenter")
        val team = createTeam(owner.id)
        val post = createPost(owner.id, team.id)
        val sibling = createPost(owner.id, team.id)

        val root = createComment(post.id, commenter.id)
        createComment(post.id, owner.id, parentId = root.id)
        createComment(sibling.id, commenter.id)

        postService.deletePost(post.id, owner)

        assertThat(
            count("SELECT COUNT(*) FROM comments WHERE post_id = ? AND deleted_at IS NULL", post.id),
        ).isZero()
        assertThat(
            count("SELECT COUNT(*) FROM comments WHERE post_id = ? AND deleted_at IS NULL", sibling.id),
        ).isEqualTo(1L)
    }

    @Test
    fun `deleteTeam은 팀의 모집글에 달린 댓글까지 정리한다`() {
        val leader = createUser("leader")
        val commenter = createUser("commenter")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        val post = createPost(leader.id, team.id)

        val root = createComment(post.id, commenter.id)
        createComment(post.id, leader.id, parentId = root.id)

        teamService.deleteTeam(team.id, leader)

        assertThat(count("SELECT COUNT(*) FROM comments WHERE deleted_at IS NULL")).isZero()
    }

    @Test
    fun `deactivateUser는 본인 댓글을 tombstone으로 남기고 남의 답글 스레드를 보존한다`() {
        val author = createUser("author")
        val replier = createUser("replier")
        val otherTeam = createTeam(replier.id)
        val post = createPost(replier.id, otherTeam.id)

        val root = createComment(post.id, author.id)
        val reply = createComment(post.id, replier.id, parentId = root.id)

        val standalone = createComment(post.id, author.id)

        userService.deactivateUser(author)

        // 답글이 없는 댓글은 앵커가 필요 없으므로 자리표시자로 남기지 않는다
        assertThat(
            count("SELECT COUNT(*) FROM comments WHERE id = ? AND deleted_at IS NULL", standalone.id),
        ).isZero()

        // 남의 답글이 고아가 되지 않도록 tombstone으로 남김
        assertThat(
            count(
                "SELECT COUNT(*) FROM comments WHERE id = ? AND deleted_at IS NULL AND tombstoned_at IS NOT NULL",
                root.id,
            ),
        ).isEqualTo(1L)
        assertThat(
            count("SELECT COUNT(*) FROM comments WHERE id = ? AND deleted_at IS NULL", reply.id),
        ).isEqualTo(1L)
    }

    @Test
    fun `deactivateUser는 빈 껍데기가 된 부모 tombstone을 남기지 않는다`() {
        val author = createUser("author")
        val other = createUser("other")
        val team = createTeam(other.id)
        val post = createPost(other.id, team.id)

        // 본인 댓글에 본인이 답글 — 답글이 지워지면 부모가 빈 tombstone이 된다
        val ownRoot = createComment(post.id, author.id)
        createComment(post.id, author.id, parentId = ownRoot.id)

        // 남의 tombstone에 남은 마지막 답글이 탈퇴자 것 — 그 답글이 지워지면 빈 껍데기가 된다
        val othersRoot = createComment(post.id, other.id)
        createComment(post.id, author.id, parentId = othersRoot.id)
        commentService.deleteComment(othersRoot.id, other) // 답글이 있어 tombstone

        val survivor = createComment(post.id, other.id)

        userService.deactivateUser(author)

        assertThat(
            count("SELECT COUNT(*) FROM comments WHERE id = ? AND deleted_at IS NULL", ownRoot.id),
        ).isZero()
        assertThat(
            count("SELECT COUNT(*) FROM comments WHERE id = ? AND deleted_at IS NULL", othersRoot.id),
        ).isZero()
        // 무관한 댓글은 건드리지 않는다
        assertThat(
            count("SELECT COUNT(*) FROM comments WHERE id = ? AND deleted_at IS NULL", survivor.id),
        ).isEqualTo(1L)
    }
}
