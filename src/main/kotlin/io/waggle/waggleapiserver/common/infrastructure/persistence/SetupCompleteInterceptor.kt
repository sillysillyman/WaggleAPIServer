package io.waggle.waggleapiserver.common.infrastructure.persistence

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.term.service.TermService
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import io.waggle.waggleapiserver.security.oauth2.UserPrincipal
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RequireCompleteSetup

@Component
class SetupCompleteInterceptor(
    private val userRepository: UserRepository,
    private val termService: TermService,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler !is HandlerMethod) return true
        if (!handler.hasMethodAnnotation(RequireCompleteSetup::class.java)) return true

        val principal =
            SecurityContextHolder.getContext().authentication?.principal as? UserPrincipal
                ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

        val user =
            userRepository.findByIdOrNull(principal.userId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "User not found: ${principal.userId}",
                )

        user.checkProfileComplete()
        if (!handler.hasMethodAnnotation(AllowMissingTermAgreement::class.java)) {
            termService.checkAllRequiredAgreed(user)
        }
        return true
    }
}
