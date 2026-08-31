package io.waggle.waggleapiserver.domain.user.service

import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.like.LikeType
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.notification.NotificationType
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify

class UserServiceCascadeTest : CascadeIntegrationTestSupport() {
    @Test
    fun `deactivateUser는 승계자 없는 리더 팀을 통째로 정리하고 refresh token을 제거한다`() {
        val user = createUser("solo")
        val applicant = createUser("applicant")

        val team = createTeam(user.id)
        createMember(user.id, team.id, MemberRole.LEADER)
        val post = createPost(user.id, team.id)
        createRecruitment(post.id)
        val application = createApplication(team.id, post.id, applicant.id)
        createApplicationRead(application.id, user.id)
        createNotification(user.id, NotificationType.TEAM_JOINED, mapOf("teamId" to team.id))

        userService.deactivateUser(user)

        assertThat(count("SELECT COUNT(*) FROM teams WHERE id = ? AND deleted_at IS NULL", team.id)).isZero()
        assertThat(count("SELECT COUNT(*) FROM members WHERE team_id = ? AND deleted_at IS NULL", team.id)).isZero()
        assertThat(count("SELECT COUNT(*) FROM posts WHERE team_id = ? AND deleted_at IS NULL", team.id)).isZero()
        assertThat(count("SELECT COUNT(*) FROM recruitments WHERE post_id = ?", post.id)).isZero()
        assertThat(count("SELECT COUNT(*) FROM notifications WHERE team_id = ?", team.id)).isZero()
        // 탈퇴자 soft-delete → applicant만 active
        assertThat(count("SELECT COUNT(*) FROM users WHERE deleted_at IS NULL")).isEqualTo(1L)

        verify(authService).deleteRefreshToken(user.id)
    }

    @Test
    fun `deactivateUser는 승계자가 있으면 팀을 유지하고 리더를 승계한다`() {
        val leader = createUser("leader")
        val successor = createUser("successor")

        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        val successorMember = createMember(successor.id, team.id, MemberRole.MEMBER)
        createPost(successor.id, team.id)

        userService.deactivateUser(leader)

        // 팀과 승계자 소유 데이터 유지
        assertThat(count("SELECT COUNT(*) FROM teams WHERE id = ? AND deleted_at IS NULL", team.id)).isEqualTo(1L)
        assertThat(count("SELECT COUNT(*) FROM posts WHERE team_id = ? AND deleted_at IS NULL", team.id)).isEqualTo(1L)
        // 탈퇴자 본인 멤버십만 soft-delete → 팀에 active member 1명(승계자)
        assertThat(
            count("SELECT COUNT(*) FROM members WHERE team_id = ? AND deleted_at IS NULL", team.id),
        ).isEqualTo(1L)

        val promoted = memberRepository.findById(successorMember.id).get()
        assertThat(promoted.role).isEqualTo(MemberRole.LEADER)
        val reloadedTeam = teamRepository.findById(team.id).get()
        assertThat(reloadedTeam.leaderId).isEqualTo(successor.id)
    }

    @Test
    fun `deactivateUser는 사용자 전역 데이터를 정리한다`() {
        val user = createUser("user")
        val other = createUser("other")

        createFollow(user.id, other.id)
        createFollow(other.id, user.id)
        createBookmark(user.id, 999L, BookmarkType.POST)
        createNotification(user.id, NotificationType.REVIEW_RECEIVED, mapOf("teamId" to 1L))
        createUserTermAgreement(user.id, 1L)
        val otherTeam = createTeam(other.id)
        createPost(user.id, otherTeam.id)

        userService.deactivateUser(user)

        assertThat(count("SELECT COUNT(*) FROM follows WHERE deleted_at IS NULL")).isZero()
        assertThat(count("SELECT COUNT(*) FROM bookmarks")).isZero()
        assertThat(count("SELECT COUNT(*) FROM notifications")).isZero()
        assertThat(count("SELECT COUNT(*) FROM user_term_agreements")).isZero()
        assertThat(count("SELECT COUNT(*) FROM posts WHERE deleted_at IS NULL")).isZero()
        assertThat(count("SELECT COUNT(*) FROM users WHERE deleted_at IS NULL")).isEqualTo(1L)
    }

    @Test
    fun `deactivateUser는 누른 좋아요와 받은 좋아요를 모두 정리한다`() {
        val user = createUser("user")
        val other = createUser("other")
        val otherTeam = createTeam(other.id)
        val othersPost = createPost(other.id, otherTeam.id)
        val ownPost = createPost(user.id, otherTeam.id)
        val ownComment = createComment(othersPost.id, user.id)
        val othersComment = createComment(othersPost.id, other.id)

        // 탈퇴자가 누른 좋아요
        createLike(user.id, LikeType.POST, othersPost.id)
        // 탈퇴자의 글·댓글이 받은 좋아요
        createLike(other.id, LikeType.POST, ownPost.id)
        createLike(other.id, LikeType.COMMENT, ownComment.id)
        // 무관한 좋아요 — 보존 대상
        createLike(other.id, LikeType.COMMENT, othersComment.id)

        userService.deactivateUser(user)

        assertThat(count("SELECT COUNT(*) FROM likes WHERE type = 'POST' AND target_id = ?", othersPost.id))
            .isZero()
        assertThat(count("SELECT COUNT(*) FROM likes WHERE type = 'POST' AND target_id = ?", ownPost.id)).isZero()
        assertThat(count("SELECT COUNT(*) FROM likes WHERE type = 'COMMENT' AND target_id = ?", ownComment.id))
            .isZero()
        assertThat(count("SELECT COUNT(*) FROM likes WHERE type = 'COMMENT' AND target_id = ?", othersComment.id))
            .isEqualTo(1L)
    }
}
