package io.waggle.waggleapiserver.common.infrastructure.discord

import io.waggle.waggleapiserver.common.util.logger
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.filter.OncePerRequestFilter

class ServletErrorAlertFilter(
    private val discordWebhookClientProvider: ObjectProvider<DiscordWebhookClient>,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            filterChain.doFilter(request, response)
        } catch (e: Exception) {
            try {
                discordWebhookClientProvider
                    .getIfAvailable()
                    ?.send(DiscordErrorContext.fromHttp(request, e))
            } catch (notifyError: Exception) {
                logger.warn(
                    "Failed to enqueue Discord notification for filter-chain exception",
                    notifyError,
                )
            }
            throw e
        }
    }
}

@Configuration
class ServletErrorAlertFilterConfig {
    @Bean
    fun servletErrorAlertFilterRegistration(
        discordWebhookClientProvider: ObjectProvider<DiscordWebhookClient>,
    ): FilterRegistrationBean<ServletErrorAlertFilter> {
        val registration = FilterRegistrationBean(ServletErrorAlertFilter(discordWebhookClientProvider))
        registration.order = Ordered.HIGHEST_PRECEDENCE
        registration.addUrlPatterns("/*")
        return registration
    }
}
