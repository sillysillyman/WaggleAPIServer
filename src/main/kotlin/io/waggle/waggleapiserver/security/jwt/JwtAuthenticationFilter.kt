package io.waggle.waggleapiserver.security.jwt

import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import io.waggle.waggleapiserver.security.oauth2.UserPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val token = extractTokenFromRequest(request)

            if (token != null && jwtProvider.isTokenValid(token)) {
                val userId = jwtProvider.getUserIdFromToken(token)

                val user = userRepository.findById(userId).orElse(null)

                if (user != null) {
                    val claims = jwtProvider.getClaimsFromToken(token)
                    val authorities = listOf(SimpleGrantedAuthority("ROLE_${claims["role"]}"))

                    val userPrincipal = UserPrincipal(user)

                    val authentication =
                        UsernamePasswordAuthenticationToken(
                            userPrincipal,
                            null,
                            authorities,
                        )

                    SecurityContextHolder.getContext().authentication = authentication
                }
            }
        } catch (e: Exception) {
            logger.error("JWT authentication failed", e)
        }

        filterChain.doFilter(request, response)
    }

    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return bearerToken?.takeIf { it.startsWith("Bearer ") }?.substring(7)
    }
}
