package me.nathanfallet.asonar.presentation.routes.health

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.nathanfallet.asonar.domain.services.HealthService

/**
 * Liveness/readiness probe. Answers 200 when the app and its database are up, 503 otherwise, with no
 * authentication so an orchestrator can always reach it.
 */
fun Route.healthRoutes(healthService: HealthService) {
    get("/health") {
        if (healthService.isHealthy()) {
            call.respondText("OK", status = HttpStatusCode.OK)
        } else {
            call.respondText("UNHEALTHY", status = HttpStatusCode.ServiceUnavailable)
        }
    }
}
