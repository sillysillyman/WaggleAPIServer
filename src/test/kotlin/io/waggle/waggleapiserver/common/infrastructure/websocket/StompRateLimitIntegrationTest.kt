package io.waggle.waggleapiserver.common.infrastructure.websocket

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.messaging.support.AbstractSubscribableChannel
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(
    properties = [
        "COOKIE_DOMAIN=localhost",
        "OAUTH2_REDIRECT_URI=http://localhost",
        "S3_BASE_URL=http://localhost",
        "S3_BUCKET=test-bucket",
        "JWT_SECRET=test-secret-test-secret-test-secret-test-secret",
        "GOOGLE_CLIENT_ID=test",
        "GOOGLE_CLIENT_SECRET=test",
        "KAKAO_CLIENT_ID=test",
        "KAKAO_CLIENT_SECRET=test",
    ],
)
@Testcontainers
@ActiveProfiles("mysql-test")
class StompRateLimitIntegrationTest
    @Autowired
    constructor(
        @Qualifier("clientInboundChannel") private val clientInboundChannel: AbstractSubscribableChannel,
    ) {
        companion object {
            @Container
            @ServiceConnection
            val mysql =
                MySQLContainer("mysql:8.0").apply {
                    withDatabaseName("waggle")
                    withCommand("--ngram-token-size=2")
                }

            @Container
            val redis: GenericContainer<*> =
                GenericContainer("redis:7-alpine").withExposedPorts(6379)

            @JvmStatic
            @DynamicPropertySource
            fun redisProperties(registry: DynamicPropertyRegistry) {
                registry.add("spring.data.redis.host") { redis.host }
                registry.add("spring.data.redis.port") { redis.firstMappedPort }
            }
        }

        @Test
        fun `StompRateLimitInterceptor가 client inbound channel에 등록됨`() {
            assertThat(clientInboundChannel.interceptors)
                .anyMatch { it is StompRateLimitInterceptor }
        }
    }
