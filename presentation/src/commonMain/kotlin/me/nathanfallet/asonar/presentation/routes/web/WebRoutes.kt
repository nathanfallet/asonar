package me.nathanfallet.asonar.presentation.routes.web

import io.ktor.http.*
import io.ktor.server.freemarker.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.keywords.KeywordDetail
import me.nathanfallet.asonar.domain.models.keywords.KeywordOverview
import me.nathanfallet.asonar.domain.models.keywords.KeywordPayload
import me.nathanfallet.asonar.domain.usecases.keywords.GetKeywordDetailUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.GetOrCreateKeywordUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.ListKeywordOverviewsUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.RefreshKeywordUseCase
import me.nathanfallet.asonar.presentation.extensions.parseStore
import me.nathanfallet.asonar.presentation.views.*
import kotlin.time.Instant

/** Use cases the web UI needs. */
data class WebRoutesDependencies(
    val listKeywordOverviewsUseCase: ListKeywordOverviewsUseCase,
    val getKeywordDetailUseCase: GetKeywordDetailUseCase,
    val getOrCreateKeywordUseCase: GetOrCreateKeywordUseCase,
    val refreshKeywordUseCase: RefreshKeywordUseCase,
    val mcpServer: Server,
)

/** The server-rendered web UI: dashboard, add-keyword form, keyword detail, and the MCP guide. */
fun Route.webRoutes(dependencies: WebRoutesDependencies) = with(dependencies) {
    get("/") {
        call.respondRedirect("/keywords")
    }
    get("/keywords") {
        val rows = listKeywordOverviewsUseCase(Pagination(limit = 1000)).map { it.toRow() }
        call.respond(
            FreeMarkerContent(
                "dashboard.ftl",
                mapOf("view" to DashboardView(LayoutView("Mots-clés", "keywords"), rows)),
            )
        )
    }
    post("/keywords") {
        val params = call.receiveParameters()
        val term = params["term"]?.takeIf { it.isNotBlank() }
        val store = params["store"]?.let { parseStore(it) }
        val country = params["country"]?.takeIf { it.isNotBlank() }
        if (term != null && store != null && country != null) {
            getOrCreateKeywordUseCase(KeywordPayload(term, store, country))
        }
        call.respondRedirect("/keywords")
    }
    get("/keywords/{id}") {
        val detail = call.parameters["id"]?.toLongOrNull()?.let { getKeywordDetailUseCase(it) }
        if (detail == null) {
            call.respond(HttpStatusCode.NotFound, "Keyword not found")
            return@get
        }
        call.respond(FreeMarkerContent("keyword.ftl", mapOf("view" to detail.toDetailView())))
    }
    post("/keywords/{id}/refresh") {
        val id = call.parameters["id"]?.toLongOrNull()
        if (id != null) refreshKeywordUseCase(id)
        call.respondRedirect(if (id != null) "/keywords/$id" else "/keywords")
    }
    get("/mcp-guide") {
        val origin = call.request.origin
        val mcpUrl = "${origin.scheme}://${origin.serverHost}:${origin.serverPort}/mcp"
        call.respond(
            FreeMarkerContent(
                "mcp.ftl",
                mapOf(
                    "view" to McpGuideView(
                        layout = LayoutView("MCP", "mcp"),
                        mcpUrl = mcpUrl,
                        claudeCodeCommand = "claude mcp add --transport http asonar $mcpUrl",
                        // Generated live from the registered tools, so it never goes stale.
                        tools = mcpServer.tools.values
                            .map { ToolInfoView(it.tool.name, it.tool.description ?: "") }
                            .sortedBy { it.name },
                    )
                ),
            )
        )
    }
}

private fun KeywordOverview.toRow() = KeywordRowView(
    id = keyword.id,
    term = keyword.term,
    store = keyword.store.name,
    country = keyword.country,
    popularityLabel = latestPopularity?.popularity?.toString() ?: "—",
    popularityValue = latestPopularity?.popularity ?: 0,
    hasPopularity = latestPopularity != null,
    capturedAt = latestPopularity?.capturedAt.formatted(),
)

private fun KeywordDetail.toDetailView() = KeywordDetailView(
    layout = LayoutView(keyword.term, "keywords"),
    id = keyword.id,
    term = keyword.term,
    store = keyword.store.name,
    country = keyword.country,
    popularityLabel = latestPopularity?.popularity?.toString() ?: "—",
    popularityValue = latestPopularity?.popularity ?: 0,
    hasPopularity = latestPopularity != null,
    capturedAt = latestPopularity?.capturedAt.formatted(),
    topApps = topApps.map {
        TopAppRowView(
            position = it.snapshot.position,
            appName = it.snapshot.appName,
            subtitle = it.snapshot.subtitle ?: "",
            storeAppId = it.snapshot.storeAppId,
            ratings = it.snapshot.ratingCount?.toString() ?: "—",
            averageRating = it.snapshot.averageRating?.let { r -> (kotlin.math.round(r * 10) / 10).toString() } ?: "—",
            reviews30d = it.ratingsPer30d?.let { v -> if (v >= 0) "+$v" else v.toString() } ?: "—",
        )
    },
    ranks = ranks.map {
        RankRowView(
            appName = it.app.name,
            rankLabel = it.rank.rank?.let { r -> "#$r" } ?: "—",
            totalResults = it.rank.totalResults?.toString() ?: "—",
            capturedAt = it.rank.capturedAt.formatted(),
        )
    },
)

private fun Instant?.formatted(): String =
    this?.toString()?.take(16)?.replace("T", " ") ?: "—"
