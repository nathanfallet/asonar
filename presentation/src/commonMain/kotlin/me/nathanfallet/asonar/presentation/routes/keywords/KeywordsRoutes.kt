package me.nathanfallet.asonar.presentation.routes.keywords

import io.ktor.http.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.nathanfallet.asonar.api.requests.keywords.TrackKeywordRequest
import me.nathanfallet.asonar.api.resources.keywords.KeywordsApi
import me.nathanfallet.asonar.api.responses.keywords.KeywordsResponse
import me.nathanfallet.asonar.api.responses.snapshots.PopularitySnapshotsResponse
import me.nathanfallet.asonar.api.responses.snapshots.RankSnapshotsResponse
import me.nathanfallet.asonar.api.responses.snapshots.TopAppSnapshotsResponse
import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.keywords.KeywordPayload
import me.nathanfallet.asonar.domain.usecases.keywords.DeleteKeywordUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.GetKeywordDetailUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.GetLatestTopAppsUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.GetOrCreateKeywordUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.ListKeywordOverviewsUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.ListPopularityHistoryUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.ListRankHistoryUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.RefreshKeywordUseCase
import me.nathanfallet.asonar.presentation.extensions.parseStore
import me.nathanfallet.asonar.presentation.mappers.keywords.toKeywordDetailResponse
import me.nathanfallet.asonar.presentation.mappers.keywords.toKeywordResponse
import me.nathanfallet.asonar.presentation.mappers.snapshots.toPopularitySnapshotResponse
import me.nathanfallet.asonar.presentation.mappers.snapshots.toRankSnapshotResponse
import me.nathanfallet.asonar.presentation.mappers.snapshots.toTopAppSnapshotResponse

/**
 * Use cases the keyword surfaces need. Shared by the HTTP routes here and the MCP tools, so both
 * speak to the domain the same way. Note there is no "record run" here: fetched data is written
 * only by the internal fetch pipeline, never through the API.
 */
data class KeywordsRoutesDependencies(
    val listKeywordOverviewsUseCase: ListKeywordOverviewsUseCase,
    val getKeywordDetailUseCase: GetKeywordDetailUseCase,
    val getOrCreateKeywordUseCase: GetOrCreateKeywordUseCase,
    val deleteKeywordUseCase: DeleteKeywordUseCase,
    val listPopularityHistoryUseCase: ListPopularityHistoryUseCase,
    val getLatestTopAppsUseCase: GetLatestTopAppsUseCase,
    val listRankHistoryUseCase: ListRankHistoryUseCase,
    val refreshKeywordUseCase: RefreshKeywordUseCase,
)

fun Route.keywordsRoutes(dependencies: KeywordsRoutesDependencies) = with(dependencies) {
    get<KeywordsApi> {
        val overviews = listKeywordOverviewsUseCase(Pagination(limit = 0))
        call.respond(KeywordsResponse(overviews.map { it.toKeywordResponse() }))
    }
    post<KeywordsApi, TrackKeywordRequest> { _, request ->
        val store = parseStore(request.store)
            ?: return@post call.respond(HttpStatusCode.BadRequest, "Unknown store: ${request.store}")
        val keyword = getOrCreateKeywordUseCase(KeywordPayload(request.term, store, request.country))
        call.respond(keyword.toKeywordResponse())
    }
    get<KeywordsApi.Id> { params ->
        val detail = getKeywordDetailUseCase(params.id)
            ?: return@get call.respond(HttpStatusCode.NotFound, "Keyword ${params.id} not found")
        call.respond(detail.toKeywordDetailResponse())
    }
    delete<KeywordsApi.Id> { params ->
        if (deleteKeywordUseCase(params.id)) call.respond(HttpStatusCode.NoContent)
        else call.respond(HttpStatusCode.NotFound, "Keyword ${params.id} not found")
    }
    get<KeywordsApi.Id.Popularity> { params ->
        val history = listPopularityHistoryUseCase(params.parent.id, Pagination(limit = 100))
        call.respond(PopularitySnapshotsResponse(history.map { it.toPopularitySnapshotResponse() }))
    }
    get<KeywordsApi.Id.TopApps> { params ->
        val top = getLatestTopAppsUseCase(params.parent.id)
        call.respond(TopAppSnapshotsResponse(top.map { it.toTopAppSnapshotResponse() }))
    }
    get<KeywordsApi.Id.Ranks> { params ->
        val history = listRankHistoryUseCase(params.parent.id, params.appId, Pagination(limit = 100))
        call.respond(RankSnapshotsResponse(history.map { it.toRankSnapshotResponse() }))
    }
    // Explicit path (the no-body typed resource POST is awkward); the client still builds this URL
    // from KeywordsApi.Id.Refresh, so the two stay in sync.
    post("/api/keywords/{id}/refresh") {
        val id = call.parameters["id"]?.toLongOrNull()
        if (id != null && refreshKeywordUseCase(id)) {
            call.respond(HttpStatusCode.Accepted, "Fetch queued for keyword $id")
        } else {
            call.respond(HttpStatusCode.NotFound, "Keyword $id not found")
        }
    }
}
