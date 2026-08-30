package io.waggle.waggleapiserver.common

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.comment.dto.request.CommentCreateRequest
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.post.dto.request.PostUpdateRequest
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull

/**
 * 엔티티의 @SQLRestriction("deleted_at IS NULL")이 soft delete된 행을 가리는지 고정하는 테스트.
 *
 * 이전 구조(@Filter + SoftDeleteFilterAspect)는 두 경로에서 뚫렸다 —
 * em.find()에는 필터가 안 붙었고, @Transactional 밖에서는 필터 자체가 안 켜졌다.
 */
class SoftDeleteRestrictionTest : CascadeIntegrationTestSupport() {
    @Test
    fun `삭제된 모집글은 findByIdOrNull에 잡히지 않는다`() {
        val owner = createUser("owner")
        val team = createTeam(owner.id)
        val post = createPost(owner.id, team.id)

        postService.deletePost(post.id, owner)

        // em.find() 경로 — 이전에는 삭제된 글이 그대로 나왔음
        assertThat(postRepository.findByIdOrNull(post.id)).isNull()
        // 트랜잭션 밖 JPQL 경로 — 이전에는 true였음
        assertThat(postRepository.existsById(post.id)).isFalse()
        // 행 자체는 남아 있음
        assertThat(count("SELECT COUNT(*) FROM posts WHERE id = ?", post.id)).isEqualTo(1L)
    }

    @Test
    fun `삭제된 모집글은 상세 조회에서 404가 된다`() {
        val owner = createUser("owner")
        val team = createTeam(owner.id)
        val post = createPost(owner.id, team.id)

        postService.deletePost(post.id, owner)

        assertThatThrownBy { postService.getPost(post.id, null) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENTITY_NOT_FOUND)
    }

    @Test
    fun `삭제된 모집글에는 댓글을 달 수 없다`() {
        val owner = createUser("owner")
        val commenter = createUser("commenter")
        val team = createTeam(owner.id)
        val post = createPost(owner.id, team.id)

        postService.deletePost(post.id, owner)

        assertThatThrownBy {
            commentService.createComment(post.id, CommentCreateRequest(null, "댓글"), commenter)
        }.isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENTITY_NOT_FOUND)
    }

    @Test
    fun `삭제된 모집글은 수정할 수 없다`() {
        val owner = createUser("owner")
        val team = createTeam(owner.id)
        createMember(owner.id, team.id, MemberRole.LEADER)
        val post = createPost(owner.id, team.id)

        postService.deletePost(post.id, owner)

        assertThatThrownBy {
            postService.updatePost(
                post.id,
                PostUpdateRequest(title = "수정", content = "수정", recruitments = emptyList()),
                owner,
            )
        }.isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENTITY_NOT_FOUND)
    }

    @Test
    fun `탈퇴한 사용자는 인증 경로에서 조회되지 않는다`() {
        val user = createUser("victim")

        userService.deactivateUser(user)

        // CurrentUserArgumentResolver가 하는 것과 같은 호출.
        // 이전에는 찾아져서 JWT 만료까지 인증이 유지됐음.
        assertThat(userRepository.findByIdOrNull(user.id)).isNull()
    }

    @Test
    fun `native 쿼리는 @SQLRestriction을 타지 않아 삭제된 행을 읽을 수 있다`() {
        val leader = createUser("leader")
        val member = createUser("member")
        val team = createTeam(leader.id)
        createMember(leader.id, team.id, MemberRole.LEADER)
        val target = createMember(member.id, team.id, MemberRole.MEMBER)

        jdbcTemplate.update("UPDATE members SET deleted_at = UTC_TIMESTAMP(6) WHERE id = ?", target.id)
        jdbcTemplate.update(
            "UPDATE users SET deleted_at = UTC_TIMESTAMP(6) WHERE id = UUID_TO_BIN(?)",
            member.id.toString(),
        )

        // 재가입 시 멤버 복원, 탈퇴자 조회 등이 의존하는 탈출구.
        assertThat(memberRepository.findByUserIdAndTeamIdAndDeletedAtIsNotNull(member.id, team.id)).isNotNull()
        assertThat(memberRepository.findByUserIdAndTeamIdIncludingDeleted(member.id, team.id)).isNotNull()
        assertThat(memberRepository.findByTeamIdAndDeletedAtIsNotNullOrderByRoleAscIdAsc(team.id)).hasSize(1)
        assertThat(userRepository.findByIdIgnoringDeletion(member.id)).isNotNull()

        // 같은 대상이 JPA 경로에서는 가려짐
        assertThat(memberRepository.findByUserIdAndTeamId(member.id, team.id)).isNull()
        assertThat(userRepository.findByIdOrNull(member.id)).isNull()
    }
}
