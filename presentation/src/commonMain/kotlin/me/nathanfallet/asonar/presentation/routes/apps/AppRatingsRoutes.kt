package me.nathanfallet.asonar.presentation.routes.apps

import io.ktor.http.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.nathanfallet.asonar.api.resources.apps.AppRatingsApi
import me.nathanfallet.asonar.domain.usecases.apps.GetAppRatingHistoryUseCase
import me.nathanfallet.asonar.presentation.extensions.parseStore
import me.nathanfallet.asonar.presentation.mappers.apps.toAppRatingHistoryResponse

/** Use cases for the app-ratings surface. Shared by the HTTP route here and the MCP tool. */
data class AppRatingsRoutesDependencies(
    val getAppRatingHistoryUseCase: GetAppRatingHistoryUseCase,
)

fun Route.appRatingsRoutes(dependencies: AppRatingsRoutesDependencies) = with(dependencies) {
    get<AppRatingsApi> { params ->
        val store = parseStore(params.store)
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Unknown store: ${params.store}")
        val history = getAppRatingHistoryUseCase(store, params.storeAppId, params.country.trim().uppercase())
        call.respond(history.toAppRatingHistoryResponse())
    }
}
