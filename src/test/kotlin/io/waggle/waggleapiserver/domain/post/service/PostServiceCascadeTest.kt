package io.waggle.waggleapiserver.domain.post.service

import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.like.LikeType
import io.waggle.waggleapiserver.domain.notification.NotificationType
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PostServiceCascadeTest : CascadeIntegrationTestSupport() {
    @Test
    fun `deletePost는 게시글 하위를 정리하고 같은 팀의 형제 게시글은 보존한다`() {
        val owner = createUser("owner")
        val applicant = createUser("applicant")
        val bookmarker = createUser("bookmarker")
        val recipient = createUser("recipient")
        val team = createTeam(owner.id)

        val post = createPost(owner.id, team.id)
        val sibling = createPost(owner.id, team.id)
        createRecruitment(post.id)
        createRecruitment(sibling.id)
        val application = createApplication(team.id, post.id, applicant.id)
        createApplicationRead(application.id, owner.id)
        createBookmark(bookmarker.id, post.id, BookmarkType.POST)
        createNotification(
            recipient.id,
            NotificationType.APPLICATION_RECEIVED,
            mapOf("teamId" to team.id, "postId" to post.id),
        )
        createNotification(
            recipient.id,
            NotificationType.APPLICATION_RECEIVED,
            mapOf("teamId" to team.id, "postId" to sibling.id),
        )

        postService.deletePost(post.id, owner)

        // 대상 게시글 하위 정리
        assertThat(count("SELECT COUNT(*) FROM posts WHERE id = ? AND deleted_at IS NULL", post.id)).isZero()
        assertThat(
            count("SELECT COUNT(*) FROM applications WHERE post_id = ? AND deleted_at IS NULL", post.id),
        ).isZero()
        assertThat(count("SELECT COUNT(*) FROM application_reads WHERE deleted_at IS NULL")).isZero()
        assertThat(count("SELECT COUNT(*) FROM recruitments WHERE post_id = ?", post.id)).isZero()
        assertThat(count("SELECT COUNT(*) FROM bookmarks WHERE target_id = ? AND type = 'POST'", post.id)).isZero()
        assertThat(count("SELECT COUNT(*) FROM notifications WHERE post_id = ?", post.id)).isZero()

        // 형제 게시글 보존
        assertThat(count("SELECT COUNT(*) FROM posts WHERE id = ? AND deleted_at IS NULL", sibling.id)).isEqualTo(1L)
        assertThat(count("SELECT COUNT(*) FROM recruitments WHERE post_id = ?", sibling.id)).isEqualTo(1L)
        assertThat(count("SELECT COUNT(*) FROM notifications WHERE post_id = ?", sibling.id)).isEqualTo(1L)
    }

    @Test
    fun `deletePost는 글과 그 글 댓글의 좋아요를 정리하고 형제 글 좋아요는 보존한다`() {
        val owner = createUser("owner")
        val liker = createUser("liker")
        val team = createTeam(owner.id)
        val post = createPost(owner.id, team.id)
        val sibling = createPost(owner.id, team.id)
        val comment = createComment(post.id, owner.id)
        val siblingComment = createComment(sibling.id, owner.id)

        createLike(liker.id, LikeType.POST, post.id)
        createLike(liker.id, LikeType.COMMENT, comment.id)
        createLike(liker.id, LikeType.POST, sibling.id)
        createLike(liker.id, LikeType.COMMENT, siblingComment.id)

        postService.deletePost(post.id, owner)

        assertThat(count("SELECT COUNT(*) FROM likes WHERE type = 'POST' AND target_id = ?", post.id)).isZero()
        assertThat(count("SELECT COUNT(*) FROM likes WHERE type = 'COMMENT' AND target_id = ?", comment.id))
            .isZero()
        assertThat(count("SELECT COUNT(*) FROM likes WHERE type = 'POST' AND target_id = ?", sibling.id))
            .isEqualTo(1L)
        assertThat(count("SELECT COUNT(*) FROM likes WHERE type = 'COMMENT' AND target_id = ?", siblingComment.id))
            .isEqualTo(1L)
    }
}
