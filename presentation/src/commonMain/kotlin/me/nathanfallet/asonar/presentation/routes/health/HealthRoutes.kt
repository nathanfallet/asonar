package me.nathanfallet.asonar.presentation.routes.health

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.nathanfallet.asonar.domain.services.HealthService

/**
 * Liveness/readiness probe. Answers 200 when the database and the message broker are up, 503 naming
 * the components that are down otherwise, with no authentication so an orchestrator can always reach
 * it.
 */
fun Route.healthRoutes(healthService: HealthService) {
    get("/health") {
        val down = healthService.check().filterValues { !it }.keys
        if (down.isEmpty()) {
            call.respondText("OK", status = HttpStatusCode.OK)
        } else {
            call.respondText("UNHEALTHY: ${down.joinToString()}", status = HttpStatusCode.ServiceUnavailable)
        }
    }
}
