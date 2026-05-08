package io.waggle.waggleapiserver.common.validation.validator

import io.waggle.waggleapiserver.common.validation.constraint.WebUrl
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.net.URI

class WebUrlValidator : ConstraintValidator<WebUrl, String?> {
    override fun isValid(
        value: String?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (value == null) return true
        val normalized = if (value.matches(SCHEME_REGEX)) value else "https://$value"
        val uri = runCatching { URI(normalized) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        val host = uri.host ?: return false
        return host.contains('.')
    }

    companion object {
        private val SCHEME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+.\\-]*://.*")
    }
}
