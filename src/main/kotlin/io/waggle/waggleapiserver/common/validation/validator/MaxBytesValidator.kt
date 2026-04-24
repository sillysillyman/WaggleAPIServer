package io.waggle.waggleapiserver.common.validation.validator

import io.waggle.waggleapiserver.common.validation.constraint.MaxBytes
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.nio.charset.Charset

class MaxBytesValidator : ConstraintValidator<MaxBytes, String?> {
    private var max: Int = 0
    private var charset: Charset = Charsets.UTF_8

    override fun initialize(constraintAnnotation: MaxBytes) {
        max = constraintAnnotation.value
        charset = Charset.forName(constraintAnnotation.charset)
    }

    override fun isValid(
        value: String?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (value == null) return true
        return value.toByteArray(charset).size <= max
    }
}
