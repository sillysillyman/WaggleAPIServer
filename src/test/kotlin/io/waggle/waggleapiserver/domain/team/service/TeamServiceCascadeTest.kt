package io.waggle.waggleapiserver.domain.team.service

import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.like.LikeType
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.notification.NotificationType
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TeamServiceCascadeTest : CascadeIntegrationTestSupport() {
    @Test
    fun `deleteTeam은 팀의 모든 하위 데이터를 정리하고 team을 soft-delete하며 다른 팀은 건드리지 않는다`() {
        val leader = createUser("leader")
        val applicant = createUser("applicant")
        val bookmarker = createUser("bookmarker")
        val recipient = createUser("recipient")

        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        val post = createPost(leader.id, team.id)
        createRecruitment(post.id)
        val application = createApplication(team.id, post.id, applicant.id)
        createApplicationRead(application.id, leader.id)
        createBookmark(bookmarker.id, team.id, BookmarkType.TEAM)
        createBookmark(bookmarker.id, post.id, BookmarkType.POST)
        createNotification(recipient.id, NotificationType.TEAM_JOINED, mapOf("teamId" to team.id))
        createNotification(
            recipient.id,
            NotificationType.APPLICATION_RECEIVED,
            mapOf("teamId" to team.id, "postId" to post.id),
        )

        val otherLeader = createUser("other")
        val otherTeam = createTeam(otherLeader.id)
        createMember(otherLeader.id, otherTeam.id, MemberRole.LEADER)
        createPost(otherLeader.id, otherTeam.id)
        createNotification(otherLeader.id, NotificationType.TEAM_JOINED, mapOf("teamId" to otherTeam.id))

        teamService.deleteTeam(team.id, leader)

        // team + soft-delete 자식
        assertThat(count("SELECT COUNT(*) FROM teams WHERE id = ? AND deleted_at IS NULL", team.id)).isZero()
        assertThat(count("SELECT COUNT(*) FROM members WHERE team_id = ? AND deleted_at IS NULL", team.id)).isZero()
        assertThat(count("SELECT COUNT(*) FROM members WHERE team_id = ?", team.id)).isEqualTo(1L)
        assertThat(count("SELECT COUNT(*) FROM posts WHERE team_id = ? AND deleted_at IS NULL", team.id)).isZero()
        assertThat(
            count("SELECT COUNT(*) FROM applications WHERE team_id = ? AND deleted_at IS NULL", team.id),
        ).isZero()
        assertThat(count("SELECT COUNT(*) FROM application_reads WHERE deleted_at IS NULL")).isZero()
        // hard-delete 자식
        assertThat(count("SELECT COUNT(*) FROM recruitments WHERE post_id = ?", post.id)).isZero()
        assertThat(count("SELECT COUNT(*) FROM bookmarks WHERE target_id = ? AND type = 'TEAM'", team.id)).isZero()
        assertThat(count("SELECT COUNT(*) FROM bookmarks WHERE target_id = ? AND type = 'POST'", post.id)).isZero()
        assertThat(count("SELECT COUNT(*) FROM notifications WHERE team_id = ?", team.id)).isZero()

        // 격리: 두 번째 팀은 그대로
        assertThat(count("SELECT COUNT(*) FROM teams WHERE id = ? AND deleted_at IS NULL", otherTeam.id)).isEqualTo(1L)
        assertThat(count("SELECT COUNT(*) FROM members WHERE team_id = ? AND deleted_at IS NULL", otherTeam.id))
            .isEqualTo(1L)
        assertThat(count("SELECT COUNT(*) FROM posts WHERE team_id = ? AND deleted_at IS NULL", otherTeam.id))
            .isEqualTo(1L)
        assertThat(count("SELECT COUNT(*) FROM notifications WHERE team_id = ?", otherTeam.id)).isEqualTo(1L)
    }

    @Test
    fun `deleteTeam은 팀 산하 글과 댓글의 좋아요를 정리한다`() {
        val leader = createUser("leader")
        val liker = createUser("liker")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        val post = createPost(leader.id, team.id)
        val comment = createComment(post.id, leader.id)

        val otherTeam = createTeam(liker.id)
        val otherPost = createPost(liker.id, otherTeam.id)

        createLike(liker.id, LikeType.POST, post.id)
        createLike(liker.id, LikeType.COMMENT, comment.id)
        createLike(leader.id, LikeType.POST, otherPost.id)

        teamService.deleteTeam(team.id, leader)

        assertThat(count("SELECT COUNT(*) FROM likes WHERE type = 'POST' AND target_id = ?", post.id)).isZero()
        assertThat(count("SELECT COUNT(*) FROM likes WHERE type = 'COMMENT' AND target_id = ?", comment.id))
            .isZero()
        assertThat(count("SELECT COUNT(*) FROM likes WHERE type = 'POST' AND target_id = ?", otherPost.id))
            .isEqualTo(1L)
    }
}
