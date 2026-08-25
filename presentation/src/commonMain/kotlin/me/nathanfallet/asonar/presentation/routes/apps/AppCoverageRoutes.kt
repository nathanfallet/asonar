package me.nathanfallet.asonar.presentation.routes.apps

import io.ktor.http.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.nathanfallet.asonar.api.resources.apps.AppCoverageApi
import me.nathanfallet.asonar.domain.usecases.apps.GetAppKeywordCoverageUseCase
import me.nathanfallet.asonar.presentation.mappers.apps.toAppKeywordCoverageResponse

/** Use cases for the app-coverage surface. Shared by the HTTP route here and the MCP tool. */
data class AppCoverageRoutesDependencies(
    val getAppKeywordCoverageUseCase: GetAppKeywordCoverageUseCase,
)

fun Route.appCoverageRoutes(dependencies: AppCoverageRoutesDependencies) = with(dependencies) {
    get<AppCoverageApi> { params ->
        val coverage = getAppKeywordCoverageUseCase(params.appId)
            ?: return@get call.respond(HttpStatusCode.NotFound, "App ${params.appId} not found")
        call.respond(coverage.toAppKeywordCoverageResponse())
    }
}
