package io.waggle.waggleapiserver.domain.post.service

import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.domain.post.dto.request.PostGetQuery
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.doThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.QueryTimeoutException
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

/**
 * 조회수는 부가 기능이므로 Redis 장애가 조회 API를 막으면 안 됨.
 * 그리고 flush가 Redis에서 증분을 걷은 뒤 DB 반영에 실패하면 그 증분이 유실되는데,
 * 이는 PostViewService에 주석으로 명시된 의도된 트레이드오프라 동작을 고정해 둠.
 */
@MockitoSpyBean(types = [StringRedisTemplate::class, PostRepository::class])
class PostViewResilienceTest : CascadeIntegrationTestSupport() {
    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @Autowired
    private lateinit var postViewScheduler: PostViewScheduler

    @BeforeEach
    fun clearViewCountKeys() {
        val keys = redisTemplate.keys("${PostViewService.VIEW_COUNT_KEY_PREFIX}*")
        if (keys.isNotEmpty()) {
            redisTemplate.delete(keys)
        }
    }

    @Test
    fun `Redis가 죽어도 상세와 목록 조회는 동작하고 DB 값만 나간다`() {
        val author = createUser("author")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)
        jdbcTemplate.update("UPDATE posts SET view_count = 7 WHERE id = ?", post.id)

        doThrow(RedisConnectionFailureException("down")).`when`(redisTemplate).opsForValue()

        assertThatCode { postService.getPost(post.id, null) }.doesNotThrowAnyException()
        assertThatCode {
            postService.getPosts(PostGetQuery(), CursorGetQuery(cursor = null), null)
        }.doesNotThrowAnyException()
        assertThatCode { postService.getTeamPosts(team.id, null) }.doesNotThrowAnyException()

        // 증분을 못 읽어도 컬럼 값은 그대로 나감
        assertThat(postService.getPost(post.id, null).viewCount).isEqualTo(7L)
        assertThat(postService.getTeamPosts(team.id, null).first().viewCount).isEqualTo(7L)
    }

    @Test
    fun `Redis가 죽어도 플러시 스케줄러는 예외를 던지지 않는다`() {
        doThrow(RedisConnectionFailureException("down"))
            .`when`(redisTemplate)
            .scan(org.mockito.ArgumentMatchers.any(ScanOptions::class.java))

        assertThatCode { postViewScheduler.flushViewCounts() }.doesNotThrowAnyException()
    }

    @Test
    fun `DB 반영에 실패하면 이미 걷은 증분은 유실된다`() {
        val author = createUser("author")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)

        repeat(3) { postService.getPost(post.id, null) }
        doThrow(QueryTimeoutException("db down")).`when`(postRepository).increaseViewCount(anyLong(), anyLong())

        assertThatCode { postViewScheduler.flushViewCounts() }.doesNotThrowAnyException()

        // getAndDelete가 이미 키를 지웠으므로 증분 3은 복구 불가 — 의도된 트레이드오프
        assertThat(redisTemplate.hasKey(PostViewService.viewCountKey(post.id))).isFalse()
        assertThat(count("SELECT view_count FROM posts WHERE id = ?", post.id)).isZero()
    }

    @Test
    fun `플러시 도중 들어온 조회는 다음 플러시에 반영된다`() {
        val author = createUser("author")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)

        repeat(2) { postService.getPost(post.id, null) }
        postViewScheduler.flushViewCounts()

        // 플러시 직후 도착한 조회는 새 키로 쌓임
        postService.getPost(post.id, null)
        postViewScheduler.flushViewCounts()

        assertThat(count("SELECT view_count FROM posts WHERE id = ?", post.id)).isEqualTo(3L)
    }
}
