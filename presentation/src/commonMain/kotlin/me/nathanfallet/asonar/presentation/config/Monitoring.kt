package me.nathanfallet.asonar.presentation.config

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import org.slf4j.event.Level
import kotlin.uuid.Uuid

/**
 * Gives every request an id (the caller's `X-Request-Id`, or a fresh one), sends it back in the
 * response, and puts it in the MDC so every log of the request prints it. The id also rides along on
 * the RabbitMQ messages the request publishes, so a queued fetch logs under the request that queued
 * it — see `mapOfRequestId` / `withRequestId` in the infrastructure.
 */
fun Application.configureMonitoring() {
    install(CallId) {
        retrieveFromHeader(HttpHeaders.XRequestId)
        generate { Uuid.random().toString() }
        // A caller-supplied id lands in logs and AMQP headers: keep it short and printable.
        verify { it.isNotBlank() && it.length <= 128 && it.all { char -> char.isLetterOrDigit() || char in "-_.:" } }
        replyToHeader(HttpHeaders.XRequestId)
    }
    install(CallLogging) {
        level = Level.INFO
        // The same key the message consumer fills, printed by logback as %X{call-id}.
        callIdMdc("call-id")
    }
}
