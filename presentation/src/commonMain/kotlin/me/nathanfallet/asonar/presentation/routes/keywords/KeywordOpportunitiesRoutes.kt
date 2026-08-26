package me.nathanfallet.asonar.presentation.routes.keywords

import io.ktor.http.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.nathanfallet.asonar.api.resources.keywords.KeywordOpportunitiesApi
import me.nathanfallet.asonar.api.responses.keywords.KeywordOpportunitiesResponse
import me.nathanfallet.asonar.domain.usecases.keywords.GetKeywordOpportunitiesUseCase
import me.nathanfallet.asonar.presentation.mappers.keywords.toKeywordOpportunityResponse

/** Use cases for the keyword-opportunities surface. Shared by the HTTP route here and the MCP tool. */
data class KeywordOpportunitiesRoutesDependencies(
    val getKeywordOpportunitiesUseCase: GetKeywordOpportunitiesUseCase,
)

fun Route.keywordOpportunitiesRoutes(dependencies: KeywordOpportunitiesRoutesDependencies) = with(dependencies) {
    get<KeywordOpportunitiesApi> { params ->
        val opportunities = getKeywordOpportunitiesUseCase(params.appId)
            ?: return@get call.respond(HttpStatusCode.NotFound, "App ${params.appId} not found")
        call.respond(KeywordOpportunitiesResponse(opportunities.map { it.toKeywordOpportunityResponse() }))
    }
}
