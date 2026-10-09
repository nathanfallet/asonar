package me.nathanfallet.asonar.presentation.routes

import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import me.nathanfallet.asonar.client.ApiClient
import me.nathanfallet.asonar.client.ApiClientImpl
import me.nathanfallet.asonar.presentation.config.configureSerialization

/**
 * Serves [routes] the way the app does (same serialization, typed resources) and hands back the real
 * [ApiClient] wired to it — so a test covers the route, the wire format and the client in one go.
 */
fun ApplicationTestBuilder.routesUnderTest(routes: Route.() -> Unit): ApiClient {
    application {
        configureSerialization()
        install(Resources)
        routing(routes)
    }
    return ApiClientImpl("", clientBuilder = ::createClient)
}
