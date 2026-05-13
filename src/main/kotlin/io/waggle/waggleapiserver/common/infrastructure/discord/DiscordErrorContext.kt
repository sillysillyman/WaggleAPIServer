package io.waggle.waggleapiserver.common.infrastructure.discord

import jakarta.servlet.http.HttpServletRequest
import java.io.PrintWriter
import java.io.StringWriter
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

sealed interface DiscordErrorContext {
    val exceptionClass: String
    val message: String
    val stackTrace: String
    val timestamp: String

    data class Http(
        override val exceptionClass: String,
        override val message: String,
        val httpMethod: String,
        val requestUri: String,
        val queryString: String?,
        val userAgent: String?,
        val referer: String?,
        val forwardedFor: String?,
        val host: String?,
        override val stackTrace: String,
        override val timestamp: String,
    ) : DiscordErrorContext

    data class Async(
        override val exceptionClass: String,
        override val message: String,
        val source: String,
        override val stackTrace: String,
        override val timestamp: String,
    ) : DiscordErrorContext

    companion object {
        private const val STACK_TRACE_LINE_LIMIT = 15
        private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

        fun fromHttp(
            request: HttpServletRequest,
            exception: Throwable,
        ): Http =
            Http(
                exceptionClass = exception.javaClass.name,
                message = exception.message ?: "No message",
                httpMethod = request.method,
                requestUri = request.requestURI,
                queryString = request.queryString,
                userAgent = request.getHeader("User-Agent"),
                referer = request.getHeader("Referer"),
                forwardedFor = request.getHeader("X-Forwarded-For"),
                host = request.getHeader("Host"),
                stackTrace = renderStackTrace(exception),
                timestamp = nowKstFormatted(),
            )

        fun fromAsync(
            source: String,
            exception: Throwable,
        ): Async =
            Async(
                exceptionClass = exception.javaClass.name,
                message = exception.message ?: "No message",
                source = source,
                stackTrace = renderStackTrace(exception),
                timestamp = nowKstFormatted(),
            )

        private fun renderStackTrace(exception: Throwable): String {
            val writer = StringWriter()
            exception.printStackTrace(PrintWriter(writer))
            return writer
                .toString()
                .lineSequence()
                .take(STACK_TRACE_LINE_LIMIT)
                .joinToString("\n")
        }

        private fun nowKstFormatted(): String = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).format(TIMESTAMP_FORMAT)
    }
}
