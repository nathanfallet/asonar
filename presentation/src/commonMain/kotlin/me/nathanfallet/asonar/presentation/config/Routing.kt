package me.nathanfallet.asonar.presentation.config

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import me.nathanfallet.asonar.presentation.routes.apps.appsRoutes
import me.nathanfallet.asonar.presentation.routes.health.healthRoutes
import me.nathanfallet.asonar.presentation.routes.keywords.keywordsRoutes
import me.nathanfallet.asonar.presentation.routes.web.webRoutes
import org.koin.ktor.ext.get

fun Application.configureRouting() {
    install(Resources)
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

        // JSON API
        appsRoutes(get())
        keywordsRoutes(get())

        // Server-rendered web UI + its assets
        webRoutes(get())
        staticResources("", "static")
    }
}
