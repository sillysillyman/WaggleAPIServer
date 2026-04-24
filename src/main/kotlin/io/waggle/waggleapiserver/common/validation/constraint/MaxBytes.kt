package io.waggle.waggleapiserver.common.validation.constraint

import io.waggle.waggleapiserver.common.validation.validator.MaxBytesValidator
import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [MaxBytesValidator::class])
annotation class MaxBytes(
    val value: Int,
    val charset: String = "UTF-8",
    val message: String = "size must be less than or equal to {value} bytes",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
