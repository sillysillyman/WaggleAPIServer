package io.waggle.waggleapiserver.domain.post.service

import io.waggle.waggleapiserver.common.util.logger
import org.springframework.dao.DataAccessException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PostViewScheduler(
    private val postViewService: PostViewService,
) {
    // 루프를 서비스 안으로 옮기면 자기 호출이 되어 프록시를 타지 않아 키마다 트랜잭션이 열리지 않음
    @Scheduled(fixedRate = 900_000)
    fun flushViewCounts() {
        val keys =
            try {
                postViewService.scanViewCountKeys()
            } catch (e: DataAccessException) {
                logger.warn("Failed to scan view count keys", e)
                return
            }

        for (key in keys) {
            try {
                postViewService.flushViewCount(key)
            } catch (e: DataAccessException) {
                logger.warn("Failed to flush view count: $key", e)
            }
        }
    }
}
