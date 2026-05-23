package io.waggle.waggleapiserver.security.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import io.waggle.waggleapiserver.common.dto.response.ErrorResponse
import io.waggle.waggleapiserver.common.exception.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class JwtAccessDeniedHandler(
    private val objectMapper: ObjectMapper,
) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        val errorCode = ErrorCode.ACCESS_DENIED
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
