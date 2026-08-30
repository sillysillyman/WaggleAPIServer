package io.waggle.waggleapiserver.common

import io.waggle.waggleapiserver.domain.application.ApplicationStatus
import io.waggle.waggleapiserver.domain.application.service.ApplicationService
import io.waggle.waggleapiserver.domain.follow.dto.request.FollowToggleRequest
import io.waggle.waggleapiserver.domain.follow.service.FollowService
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

/**
 * soft delete된 행을 되살리는 경로가 @SQLRestriction 아래에서도 동작하는지 확인.
 * 복원은 native로 행을 찾은 뒤 deletedAt을 null로 되돌리는 구조라,
 * restriction이 그 경로를 막으면 재가입·재팔로우가 조용히 깨진다.
 */
class ReactivationRegressionTest : CascadeIntegrationTestSupport() {
    @Autowired
    private lateinit var followService: FollowService

    @Autowired
    private lateinit var applicationService: ApplicationService

    @Test
    fun `탈퇴 cascade로 지워진 팔로우를 다시 누르면 되살아난다`() {
        val follower = createUser("follower")
        val followee = createUser("followee")
        val request = FollowToggleRequest(followee.id)

        followService.toggleFollow(request, follower)
        // 언팔로우는 hard delete라 이 경로가 아님. 탈퇴 cascade가 만드는 상태를 재현.
        jdbcTemplate.update(
            "UPDATE follows SET deleted_at = UTC_TIMESTAMP(6) WHERE follower_id = UUID_TO_BIN(?)",
            follower.id.toString(),
        )

        assertThat(followService.toggleFollow(request, follower).following).isTrue()
        assertThat(
            count(
                "SELECT COUNT(*) FROM follows WHERE follower_id = UUID_TO_BIN(?) AND deleted_at IS NULL",
                follower.id.toString(),
            ),
        ).isEqualTo(1L)
    }

    @Test
    fun `팀을 나갔던 사용자의 지원을 승인하면 멤버가 되살아난다`() {
        val leader = createUser("leader")
        val rejoiner = createUser("rejoiner")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        val oldMember = createMember(rejoiner.id, team.id, MemberRole.MEMBER)
        val post = createPost(leader.id, team.id)

        // 탈퇴 상태 재현
        jdbcTemplate.update("UPDATE members SET deleted_at = UTC_TIMESTAMP(6) WHERE id = ?", oldMember.id)

        val application = createApplication(team.id, post.id, rejoiner.id)

        // 실제 프로덕션 경로 — 내부에서 findByUserIdAndTeamIdIncludingDeleted + reactivate()
        applicationService.updateApplicationStatus(application.id, ApplicationStatus.APPROVED, leader)

        assertThat(count("SELECT COUNT(*) FROM members WHERE id = ? AND deleted_at IS NULL", oldMember.id))
            .isEqualTo(1L)
        assertThat(count("SELECT COUNT(*) FROM members WHERE team_id = ?", team.id)).isEqualTo(2L)
    }

    @Test
    @Transactional
    fun `탈퇴한 사용자를 되살리면 다시 조회된다`() {
        val user = createUser("rejoiner")
        userService.deactivateUser(user)

        // CustomOAuth2UserService와 같은 순서 — 같은 트랜잭션 안에서 native 조회 후 복원
        val deleted = userRepository.findByProviderAndProviderIdAndDeletedAtIsNotNull("google", "rejoiner")
        assertThat(deleted).isNotNull()
        deleted!!.reactivate()
        followRepository.updateDeletedAtNullByFollowerIdOrFolloweeIdAndDeletedAtIsNotNull(deleted.id)
        userRepository.save(deleted)

        assertThat(userRepository.findById(user.id).orElse(null)).isNotNull()
    }
}
