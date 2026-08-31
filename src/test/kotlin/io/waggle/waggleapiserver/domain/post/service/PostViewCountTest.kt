package io.waggle.waggleapiserver.domain.post.service

import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.domain.post.dto.request.PostGetQuery
import io.waggle.waggleapiserver.support.CascadeIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate

class PostViewCountTest : CascadeIntegrationTestSupport() {
    @Autowired
    private lateinit var postViewScheduler: PostViewScheduler

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @BeforeEach
    fun clearViewCountKeys() {
        val keys = redisTemplate.keys("${PostViewService.VIEW_COUNT_KEY_PREFIX}*")
        if (keys.isNotEmpty()) {
            redisTemplate.delete(keys)
        }
    }

    @Test
    fun `상세 조회는 조회수를 올리고 응답에 즉시 반영하되 DB는 건드리지 않는다`() {
        val author = createUser("author")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)

        val first = postService.getPost(post.id, null)
        val second = postService.getPost(post.id, null)

        assertThat(first.viewCount).isEqualTo(1L)
        assertThat(second.viewCount).isEqualTo(2L)
        assertThat(count("SELECT view_count FROM posts WHERE id = ?", post.id)).isZero()
    }

    @Test
    fun `스케줄러는 Redis 증분을 DB로 옮기고 키를 비운다`() {
        val author = createUser("author")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)

        repeat(3) { postService.getPost(post.id, null) }

        postViewScheduler.flushViewCounts()

        assertThat(count("SELECT view_count FROM posts WHERE id = ?", post.id)).isEqualTo(3L)
        assertThat(redisTemplate.hasKey(PostViewService.viewCountKey(post.id))).isFalse()
        assertThat(postService.getPost(post.id, null).viewCount).isEqualTo(4L)
    }

    @Test
    fun `조회수 반영은 updated_at을 건드리지 않는다`() {
        val author = createUser("author")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)

        val before =
            jdbcTemplate.queryForObject(
                "SELECT updated_at FROM posts WHERE id = ?",
                String::class.java,
                post.id,
            )

        postService.getPost(post.id, null)
        postViewScheduler.flushViewCounts()

        val after =
            jdbcTemplate.queryForObject(
                "SELECT updated_at FROM posts WHERE id = ?",
                String::class.java,
                post.id,
            )

        assertThat(count("SELECT view_count FROM posts WHERE id = ?", post.id)).isEqualTo(1L)
        assertThat(after).isEqualTo(before)
    }

    @Test
    fun `목록 조회도 미반영 증분을 합산한다`() {
        val author = createUser("author")
        val team = createTeam(author.id)
        val post = createPost(author.id, team.id)

        repeat(2) { postService.getPost(post.id, null) }

        val posts = postService.getPosts(PostGetQuery(), CursorGetQuery(cursor = null), null)

        assertThat(posts.data.first { it.id == post.id }.viewCount).isEqualTo(2L)
    }
}
