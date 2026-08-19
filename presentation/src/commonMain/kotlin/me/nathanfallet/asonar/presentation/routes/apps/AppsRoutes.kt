package me.nathanfallet.asonar.presentation.routes.apps

import io.ktor.http.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.nathanfallet.asonar.api.requests.apps.RegisterAppRequest
import me.nathanfallet.asonar.api.resources.apps.AppsApi
import me.nathanfallet.asonar.api.responses.apps.AppsResponse
import me.nathanfallet.asonar.domain.models.apps.AppPayload
import me.nathanfallet.asonar.domain.usecases.apps.DeleteAppUseCase
import me.nathanfallet.asonar.domain.usecases.apps.GetAppUseCase
import me.nathanfallet.asonar.domain.usecases.apps.GetOrCreateAppUseCase
import me.nathanfallet.asonar.domain.usecases.apps.ListAppsUseCase
import me.nathanfallet.asonar.presentation.extensions.parseStore
import me.nathanfallet.asonar.presentation.mappers.apps.toAppResponse

/** Use cases the app surfaces need. Shared by the HTTP routes here and the MCP tools. */
data class AppsRoutesDependencies(
    val listAppsUseCase: ListAppsUseCase,
    val getAppUseCase: GetAppUseCase,
    val getOrCreateAppUseCase: GetOrCreateAppUseCase,
    val deleteAppUseCase: DeleteAppUseCase,
)

fun Route.appsRoutes(dependencies: AppsRoutesDependencies) = with(dependencies) {
    get<AppsApi> {
        call.respond(AppsResponse(listAppsUseCase().map { it.toAppResponse() }))
    }
    post<AppsApi, RegisterAppRequest> { _, request ->
        val store = parseStore(request.store)
            ?: return@post call.respond(HttpStatusCode.BadRequest, "Unknown store: ${request.store}")
        val app = getOrCreateAppUseCase(AppPayload(store, request.storeAppId, request.name))
        call.respond(app.toAppResponse())
    }
    get<AppsApi.Id> { params ->
        val app = getAppUseCase(params.id)
            ?: return@get call.respond(HttpStatusCode.NotFound, "App ${params.id} not found")
        call.respond(app.toAppResponse())
    }
    delete<AppsApi.Id> { params ->
        if (deleteAppUseCase(params.id)) call.respond(HttpStatusCode.NoContent)
        else call.respond(HttpStatusCode.NotFound, "App ${params.id} not found")
    }
}
