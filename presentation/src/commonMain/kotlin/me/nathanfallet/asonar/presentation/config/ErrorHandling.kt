package me.nathanfallet.asonar.presentation.config

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error", cause)
            call.respondText(
                text = cause.message ?: "Internal server error",
                status = HttpStatusCode.InternalServerError,
            )
        }
    }
}
