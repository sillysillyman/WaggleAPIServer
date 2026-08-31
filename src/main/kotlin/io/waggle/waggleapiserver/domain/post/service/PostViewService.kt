package io.waggle.waggleapiserver.domain.post.service

import io.waggle.waggleapiserver.common.util.logger
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostViewService(
    private val postRepository: PostRepository,
    private val redisTemplate: StringRedisTemplate,
) {
    fun incrementViewCount(postId: Long): Long =
        try {
            redisTemplate.opsForValue().increment(viewCountKey(postId)) ?: 0
        } catch (e: DataAccessException) {
            logger.warn("Failed to increment view count: $postId", e)
            0
        }

    fun getPendingViewCount(postId: Long): Long =
        try {
            redisTemplate.opsForValue().get(viewCountKey(postId))?.toLongOrNull() ?: 0
        } catch (e: DataAccessException) {
            logger.warn("Failed to read pending view count: $postId", e)
            0
        }

    fun getPendingViewCountByPostId(postIds: List<Long>): Map<Long, Long> {
        if (postIds.isEmpty()) return emptyMap()

        return try {
            val values = redisTemplate.opsForValue().multiGet(postIds.map { viewCountKey(it) })
            postIds
                .mapIndexedNotNull { index, postId ->
                    values?.get(index)?.toLongOrNull()?.let { postId to it }
                }.toMap()
        } catch (e: DataAccessException) {
            logger.warn("Failed to read pending view counts", e)
            emptyMap()
        }
    }

    fun scanViewCountKeys(): List<String> =
        redisTemplate
            .scan(
                ScanOptions
                    .scanOptions()
                    .match("$VIEW_COUNT_KEY_PREFIX*")
                    .count(100)
                    .build(),
            ).use { it.asSequence().toList() }

    // getAndDelete 성공 직후 UPDATE 전에 죽으면 그 증분은 복구 불가. 조회수는 근사치라 감수함
    @Transactional
    fun flushViewCount(key: String) {
        val postId = key.removePrefix(VIEW_COUNT_KEY_PREFIX).toLongOrNull() ?: return
        val delta = redisTemplate.opsForValue().getAndDelete(key)?.toLongOrNull() ?: return
        if (delta > 0) {
            postRepository.increaseViewCount(postId, delta)
        }
    }

    companion object {
        const val VIEW_COUNT_KEY_PREFIX = "post-view:"

        fun viewCountKey(postId: Long): String = "$VIEW_COUNT_KEY_PREFIX$postId"
    }
}
