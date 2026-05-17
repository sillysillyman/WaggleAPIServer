package io.waggle.waggleapiserver.common.infrastructure.config

import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUserArgumentResolver
import io.waggle.waggleapiserver.common.infrastructure.persistence.SetupCompleteInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val currentUserArgumentResolver: CurrentUserArgumentResolver,
    private val setupCompleteInterceptor: SetupCompleteInterceptor,
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentUserArgumentResolver)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(setupCompleteInterceptor)
    }
}
