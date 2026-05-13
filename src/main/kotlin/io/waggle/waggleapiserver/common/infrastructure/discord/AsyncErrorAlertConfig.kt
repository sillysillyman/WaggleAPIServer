package io.waggle.waggleapiserver.common.infrastructure.discord

import io.waggle.waggleapiserver.common.util.logger
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.config.ScheduledTaskRegistrar

@Configuration
class AsyncErrorAlertConfig(
    private val discordWebhookClientProvider: ObjectProvider<DiscordWebhookClient>,
) : AsyncConfigurer,
    SchedulingConfigurer {
    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler =
        AsyncUncaughtExceptionHandler { throwable, method, _ ->
            val source = "@Async ${method.declaringClass.simpleName}.${method.name}"
            logger.error("Uncaught async exception in $source", throwable)
            notifyDiscord(source, throwable)
        }

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        val scheduler =
            ThreadPoolTaskScheduler().apply {
                poolSize = SCHEDULER_POOL_SIZE
                setThreadNamePrefix("scheduled-")
                setErrorHandler { throwable ->
                    logger.error("Uncaught scheduled task exception", throwable)
                    notifyDiscord("@Scheduled task", throwable)
                }
                initialize()
            }
        taskRegistrar.setTaskScheduler(scheduler)
    }

    private fun notifyDiscord(
        source: String,
        throwable: Throwable,
    ) {
        try {
            discordWebhookClientProvider
                .getIfAvailable()
                ?.send(DiscordErrorContext.fromAsync(source, throwable))
        } catch (notifyError: Exception) {
            logger.warn("Failed to enqueue Discord notification for async exception", notifyError)
        }
    }

    companion object {
        private const val SCHEDULER_POOL_SIZE = 2
    }
}
