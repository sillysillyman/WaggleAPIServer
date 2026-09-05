package io.waggle.waggleapiserver.domain.auth

import com.fasterxml.jackson.databind.ObjectMapper
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.domain.auth.dto.request.OttRedeemRequest
import io.waggle.waggleapiserver.domain.auth.service.AuthService
import io.waggle.waggleapiserver.domain.user.UserRole
import io.waggle.waggleapiserver.security.jwt.JwtProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.testcontainers.containers.GenericContainer
import java.util.UUID

class AuthOttRedeemTest {
    companion object {
        // CascadeIntegrationTestSupport 와 같은 수동 start 싱글턴이라 정리는 Ryuk 가 맡음
        private val redis: GenericContainer<*> = GenericContainer("redis:7-alpine").withExposedPorts(6379)

        init {
            redis.start()
        }

        private const val ACCESS_TOKEN_TTL = 3_600_000L
        private const val REFRESH_TOKEN_TTL = 604_800_000L
    }

    private val redisTemplate =
        StringRedisTemplate(
            LettuceConnectionFactory(
                RedisStandaloneConfiguration(redis.host, redis.firstMappedPort),
            ).apply { afterPropertiesSet() },
        ).apply { afterPropertiesSet() }

    private val authService =
        AuthService(
            REFRESH_TOKEN_TTL,
            AuthCookieManager(cookieSecure = false, cookieSameSite = "Lax", cookieDomain = null),
            JwtProvider("test-secret-test-secret-test-secret-test-secret", ACCESS_TOKEN_TTL, REFRESH_TOKEN_TTL),
            redisTemplate,
        )

    private val mockMvc = MockMvcBuilders.standaloneSetup(AuthController(authService)).build()

    @Test
    fun `OTT 발급은 액세스와 리프레시 토큰을 한 쌍으로 저장함`() {
        val userId = UUID.randomUUID()

        val ott = authService.issueOttForOAuth(userId, UserRole.USER)

        val pairedRefreshToken = redisTemplate.opsForValue().get("oauth-ott-refresh:$ott")
        assertThat(redisTemplate.opsForValue().get("oauth-ott:$ott")).isNotNull()
        // 세션 슬롯과 같은 값이어야 교환한 클라이언트가 곧바로 refresh 할 수 있음
        assertThat(pairedRefreshToken).isEqualTo(redisTemplate.opsForValue().get("refresh-token:$userId"))
    }

    @Test
    fun `redeem 응답이 리프레시 쿠키를 내려줌`() {
        val ott = authService.issueOttForOAuth(UUID.randomUUID(), UserRole.USER)
        val issuedRefreshToken = redisTemplate.opsForValue().get("oauth-ott-refresh:$ott")

        val response =
            mockMvc
                .perform(
                    post("/auth/oauth/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ObjectMapper().writeValueAsString(OttRedeemRequest(ott))),
                ).andReturn()
                .response

        assertThat(response.status).isEqualTo(200)

        val setCookie = response.getHeader("Set-Cookie")
        assertThat(setCookie).contains("refreshToken=$issuedRefreshToken")
        assertThat(setCookie).contains("HttpOnly")
        assertThat(setCookie).contains("Path=/auth")
        assertThat(setCookie).contains("Max-Age=${REFRESH_TOKEN_TTL / 1000}")
    }

    @Test
    fun `교환한 OTT 는 두 키가 함께 소비되어 재사용되지 않음`() {
        val ott = authService.issueOttForOAuth(UUID.randomUUID(), UserRole.USER)

        authService.redeemOtt(ott, MockHttpServletResponse())

        assertThat(redisTemplate.opsForValue().get("oauth-ott:$ott")).isNull()
        assertThat(redisTemplate.opsForValue().get("oauth-ott-refresh:$ott")).isNull()
        assertThatThrownBy { authService.redeemOtt(ott, MockHttpServletResponse()) }
            .isInstanceOf(BusinessException::class.java)
    }

    @Test
    fun `짝 리프레시 토큰이 없으면 쿠키 없는 반쪽 로그인 대신 실패함`() {
        val ott = authService.issueOttForOAuth(UUID.randomUUID(), UserRole.USER)
        redisTemplate.delete("oauth-ott-refresh:$ott")
        val response = MockHttpServletResponse()

        assertThatThrownBy { authService.redeemOtt(ott, response) }
            .isInstanceOf(BusinessException::class.java)
        assertThat(response.getHeader("Set-Cookie")).isNull()
    }
}
