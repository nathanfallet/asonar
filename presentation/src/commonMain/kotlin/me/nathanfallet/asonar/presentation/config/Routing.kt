package me.nathanfallet.asonar.presentation.config

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import me.nathanfallet.asonar.presentation.routes.health.healthRoutes
import org.koin.ktor.ext.get

fun Application.configureRouting() {
    install(IgnoreTrailingSlash)
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        anyHost()
    }
    routing {
        // Probe, which must answer without any authentication
        healthRoutes(get())

        // Keyword-tracking API routes go here as we build them.
    }
}
