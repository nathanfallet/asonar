package me.nathanfallet.asonar.presentation.routes.keywords

import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.nathanfallet.asonar.api.resources.keywords.KeywordsApi
import me.nathanfallet.asonar.api.responses.keywords.KeywordsResponse
import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.usecases.keywords.ListKeywordOverviewsUseCase
import me.nathanfallet.asonar.presentation.mappers.keywords.toKeywordResponse

/**
 * Use cases the keyword surfaces need. Shared by the HTTP routes here and the MCP tools, so both
 * speak to the domain the same way.
 */
data class KeywordsRoutesDependencies(
    val listKeywordOverviewsUseCase: ListKeywordOverviewsUseCase,
)

fun Route.keywordsRoutes(dependencies: KeywordsRoutesDependencies) = with(dependencies) {
    get<KeywordsApi> {
        val overviews = listKeywordOverviewsUseCase(Pagination(limit = 100))
        call.respond(KeywordsResponse(overviews.map { it.toKeywordResponse() }))
    }
}
