package me.nathanfallet.asonar.presentation.routes.keywords

import io.ktor.http.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.nathanfallet.asonar.api.requests.keywords.DiscoverKeywordsRequest
import me.nathanfallet.asonar.api.requests.keywords.ReviewKeywordCandidatesRequest
import me.nathanfallet.asonar.api.resources.keywords.KeywordCandidatesApi
import me.nathanfallet.asonar.api.responses.keywords.KeywordCandidatesResponse
import me.nathanfallet.asonar.api.responses.keywords.KeywordDiscoveryResponse
import me.nathanfallet.asonar.api.responses.keywords.KeywordsResponse
import me.nathanfallet.asonar.domain.usecases.keywords.DiscoverKeywordCandidatesUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.ListKeywordCandidatesUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.ReviewKeywordCandidatesUseCase
import me.nathanfallet.asonar.presentation.extensions.parseCandidateStatuses
import me.nathanfallet.asonar.presentation.mappers.keywords.toKeywordCandidateResponse
import me.nathanfallet.asonar.presentation.mappers.keywords.toKeywordResponse

/** Use cases for the keyword-discovery surface. Shared by the HTTP routes here and the MCP tools. */
data class KeywordCandidatesRoutesDependencies(
    val discoverKeywordCandidatesUseCase: DiscoverKeywordCandidatesUseCase,
    val listKeywordCandidatesUseCase: ListKeywordCandidatesUseCase,
    val reviewKeywordCandidatesUseCase: ReviewKeywordCandidatesUseCase,
)

fun Route.keywordCandidatesRoutes(dependencies: KeywordCandidatesRoutesDependencies) = with(dependencies) {
    get<KeywordCandidatesApi> { params ->
        val statuses = parseCandidateStatuses(params.status)
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Unknown status: ${params.status}")
        val candidates = listKeywordCandidatesUseCase(params.appId, statuses, params.minPopularity)
            ?: return@get call.respond(HttpStatusCode.NotFound, "App ${params.appId} not found")
        call.respond(KeywordCandidatesResponse(candidates.map { it.toKeywordCandidateResponse() }))
    }
    // A discovery pass is a write (it creates candidates) and it goes out to the sources, so it is a
    // POST rather than a side-effecting GET.
    post<KeywordCandidatesApi, DiscoverKeywordsRequest> { params, request ->
        val result = discoverKeywordCandidatesUseCase(params.appId, request.countries, request.seeds)
            ?: return@post call.respond(HttpStatusCode.NotFound, "App ${params.appId} not found")
        call.respond(
            KeywordDiscoveryResponse(
                created = result.created.map { it.toKeywordCandidateResponse() },
                updated = result.updated.map { it.toKeywordCandidateResponse() },
            )
        )
    }
    post<KeywordCandidatesApi.Review, ReviewKeywordCandidatesRequest> { _, request ->
        reviewKeywordCandidatesUseCase.dismiss(request.dismiss)
        val tracked = reviewKeywordCandidatesUseCase.accept(request.accept)
        call.respond(KeywordsResponse(tracked.map { it.toKeywordResponse() }))
    }
}
