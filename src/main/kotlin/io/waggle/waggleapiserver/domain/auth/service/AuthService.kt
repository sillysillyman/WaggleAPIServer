package io.waggle.waggleapiserver.domain.auth.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.auth.AuthCookieManager
import io.waggle.waggleapiserver.domain.auth.dto.response.AccessTokenResponse
import io.waggle.waggleapiserver.domain.auth.dto.response.WsTokenResponse
import io.waggle.waggleapiserver.domain.user.UserRole
import io.waggle.waggleapiserver.security.jwt.JwtProvider
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

@Service
class AuthService(
    @Value("\${jwt.refresh-token-ttl}") private val refreshTokenTtl: Long,
    private val authCookieManager: AuthCookieManager,
    private val jwtProvider: JwtProvider,
    private val redisTemplate: StringRedisTemplate,
) {
    fun issueOttForOAuth(
        userId: UUID,
        role: UserRole,
        response: HttpServletResponse,
    ): String {
        val accessToken = jwtProvider.generateAccessToken(userId, role)
        val refreshToken = jwtProvider.generateRefreshToken(userId, role)

        redisTemplate.opsForValue().set(
            "refresh-token:$userId",
            refreshToken,
            Duration.ofMillis(refreshTokenTtl),
        )

        val maxAgeSeconds = (refreshTokenTtl / 1000).toInt()
        authCookieManager.addRefreshTokenCookie(response, refreshToken, maxAgeSeconds)

        val ott = UUID.randomUUID().toString()
        redisTemplate.opsForValue().set(
            "oauth-ott:$ott",
            accessToken,
            Duration.ofMinutes(1),
        )

        return ott
    }

    fun redeemOtt(ott: String): AccessTokenResponse {
        val accessToken =
            redisTemplate.opsForValue().getAndDelete("oauth-ott:$ott")
                ?: throw BusinessException(
                    ErrorCode.OAUTH_OTT_INVALID,
                    "Invalid or expired OAuth one-time token",
                )
        return AccessTokenResponse(accessToken)
    }

    fun refresh(
        refreshToken: String,
        response: HttpServletResponse,
    ): AccessTokenResponse {
        if (!jwtProvider.isTokenValid(refreshToken)) {
            throw BusinessException(ErrorCode.INVALID_TOKEN, "Invalid refresh token")
        }

        val userId = jwtProvider.getUserIdFromToken(refreshToken)
        val role = jwtProvider.getRoleFromToken(refreshToken)

        val stored =
            redisTemplate.opsForValue().get("refresh-token:$userId")
                ?: throw BusinessException(ErrorCode.INVALID_TOKEN, "Refresh token not found")

        if (stored != refreshToken) {
            // 재사용 탐지 시 강제 로그아웃
            redisTemplate.delete("refresh-token:$userId")
            throw BusinessException(ErrorCode.INVALID_TOKEN, "Refresh token reuse detected")
        }

        val newAccessToken = jwtProvider.generateAccessToken(userId, role)
        val newRefreshToken = jwtProvider.generateRefreshToken(userId, role)

        redisTemplate.opsForValue().set(
            "refresh-token:$userId",
            newRefreshToken,
            Duration.ofMillis(refreshTokenTtl),
        )
        val maxAgeSeconds = (refreshTokenTtl / 1000).toInt()
        authCookieManager.addRefreshTokenCookie(response, newRefreshToken, maxAgeSeconds)

        return AccessTokenResponse(newAccessToken)
    }

    fun logout(
        refreshToken: String,
        response: HttpServletResponse,
    ) {
        if (jwtProvider.isTokenValid(refreshToken)) {
            val userId = jwtProvider.getUserIdFromToken(refreshToken)
            redisTemplate.delete("refresh-token:$userId")
        }

        authCookieManager.expireRefreshTokenCookie(response)
    }

    fun issueWsToken(userId: UUID): WsTokenResponse {
        val token = UUID.randomUUID().toString()
        redisTemplate.opsForValue().set(
            "ws-token:$token",
            userId.toString(),
            Duration.ofMinutes(1),
        )
        return WsTokenResponse(token)
    }

    fun deleteRefreshToken(userId: UUID) {
        redisTemplate.delete("refresh-token:$userId")
    }

    fun validateAndConsumeWsToken(token: String): UUID {
        val key = "ws-token:$token"
        val userId =
            redisTemplate.opsForValue().getAndDelete(key)
                ?: throw BusinessException(
                    ErrorCode.INVALID_TOKEN,
                    "Invalid or expired WebSocket token",
                )
        return UUID.fromString(userId)
    }
}
