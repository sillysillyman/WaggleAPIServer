package io.waggle.waggleapiserver.common.validation.constraint

import io.waggle.waggleapiserver.common.validation.validator.WebUrlValidator
import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [WebUrlValidator::class])
annotation class WebUrl(
    val message: String = "must be a valid web URL",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
