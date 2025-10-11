package io.waggle.waggleapiserver.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import io.waggle.waggleapiserver.common.config.logger
import io.waggle.waggleapiserver.domain.user.UserRole
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID

@Service
class JwtProvider(
    @Value("\${jwt.secret}") private val jwtSecret: String,
    @Value("\${jwt.access-token-expiration}") private val accessTokenExpiration: Long,
    @Value("\${jwt.refresh-token-expiration}") private val refreshTokenExpiration: Long,
) {
    private val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray())

    fun generateAccessToken(
        userId: String,
        email: String,
        role: UserRole,
    ): String {
        val now = Date()
        val expiration = Date(now.time + accessTokenExpiration)

        return Jwts
            .builder()
            .subject(userId)
            .claim("email", email)
            .claim("role", role.name)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(key)
            .compact()
    }

    fun generateRefreshToken(userId: String): String {
        val now = Date()
        val expiration = Date(now.time + refreshTokenExpiration)

        return Jwts
            .builder()
            .subject(userId)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(key)
            .compact()
    }

    fun isTokenValid(token: String): Boolean =
        try {
            Jwts
                .parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: SecurityException) {
            logger.error("Invalid JWT signature", e)
            false
        } catch (e: MalformedJwtException) {
            logger.error("Invalid JWT token", e)
            false
        } catch (e: ExpiredJwtException) {
            logger.error("Expired JWT token", e)
            false
        } catch (e: UnsupportedJwtException) {
            logger.error("Unsupported JWT token", e)
            false
        } catch (e: IllegalArgumentException) {
            logger.error("JWT claims string is empty", e)
            false
        }

    fun getUserIdFromToken(token: String): UUID {
        val claims = getClaimsFromToken(token)
        return UUID.fromString(claims.subject)
    }

    fun getClaimsFromToken(token: String): Claims =
        Jwts
            .parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}
