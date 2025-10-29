package io.waggle.waggleapiserver.common.exception

import io.waggle.waggleapiserver.common.dto.ErrorResponse
import jakarta.persistence.EntityNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(e: EntityNotFoundException): ErrorResponse =
        ErrorResponse(HttpStatus.NOT_FOUND.value(), e::class.simpleName!!, e.message!!)
}
