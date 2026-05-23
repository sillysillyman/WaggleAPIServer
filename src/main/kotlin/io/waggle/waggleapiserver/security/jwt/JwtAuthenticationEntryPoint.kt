package io.waggle.waggleapiserver.security.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import io.waggle.waggleapiserver.common.dto.response.ErrorResponse
import io.waggle.waggleapiserver.common.exception.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class JwtAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        val errorCode = ErrorCode.UNAUTHORIZED
        response.status = errorCode.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(
            objectMapper.writeValueAsString(
                ErrorResponse(
                    status = errorCode.status.value(),
                    code = errorCode.name,
                    message = errorCode.message,
                ),
            ),
        )
    }
}
