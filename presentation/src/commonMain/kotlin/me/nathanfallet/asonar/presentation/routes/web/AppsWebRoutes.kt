package me.nathanfallet.asonar.presentation.routes.web

import io.ktor.http.*
import io.ktor.server.freemarker.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.nathanfallet.asonar.domain.models.apps.AppKeywordCoverage
import me.nathanfallet.asonar.domain.models.apps.RankPoint
import me.nathanfallet.asonar.domain.usecases.apps.GetAppKeywordCoverageUseCase
import me.nathanfallet.asonar.domain.usecases.apps.ListAppsUseCase
import me.nathanfallet.asonar.presentation.views.*
import kotlin.math.round
import kotlin.time.Instant

/** Use cases the apps tab needs. */
data class AppsWebRoutesDependencies(
    val listAppsUseCase: ListAppsUseCase,
    val getAppKeywordCoverageUseCase: GetAppKeywordCoverageUseCase,
)

/** The "Apps" tab: pick an app, then see its ranking coverage across every tracked keyword. */
fun Route.appsWebRoutes(dependencies: AppsWebRoutesDependencies) = with(dependencies) {
    get("/apps") {
        val apps = listAppsUseCase().map { AppOptionView(it.id, it.name, it.store.name, it.storeAppId) }
        call.respond(
            FreeMarkerContent("apps.ftl", mapOf("view" to AppsListView(LayoutView("Apps", "apps"), apps))),
        )
    }
    get("/apps/{id}") {
        val coverage = call.parameters["id"]?.toLongOrNull()?.let { getAppKeywordCoverageUseCase(it) }
        if (coverage == null) {
            call.respond(HttpStatusCode.NotFound, "App not found")
            return@get
        }
        call.respond(FreeMarkerContent("app.ftl", mapOf("view" to coverage.toCoverageView())))
    }
}

private fun AppKeywordCoverage.toCoverageView() = AppCoverageView(
    layout = LayoutView(app.name, "apps"),
    appName = app.name,
    store = app.store.name,
    storeAppId = app.storeAppId,
    rankedCount = entries.count { it.rank != null },
    totalCount = entries.size,
    rows = entries.map { entry ->
        CoverageRowView(
            keywordId = entry.keyword.id,
            term = entry.keyword.term,
            country = entry.keyword.country,
            popularityLabel = entry.popularity?.toString() ?: "—",
            rankLabel = entry.rank?.let { "#$it" } ?: "—",
            ranked = entry.rank != null,
            sparkPoints = sparkline(entry.history),
            capturedAt = entry.capturedAt.formatted(),
        )
    },
)

/**
 * An SVG polyline (points for a `0 0 100 24` viewBox) of the rank over time. Only ranked readings are
 * plotted; a better (lower) rank sits higher on the chart. Returns "" with fewer than two points, so
 * the template can fall back to a dash.
 */
private fun sparkline(history: List<RankPoint>): String {
    val ranks = history.mapNotNull { it.rank }
    if (ranks.size < 2) return ""
    val min = ranks.min()
    val max = ranks.max()
    val w = 100.0
    val h = 24.0
    val pad = 3.0
    val n = ranks.size
    return ranks.mapIndexed { i, r ->
        val x = pad + i.toDouble() / (n - 1) * (w - 2 * pad)
        // (r - min)/(max - min): best rank → 0 (top), worst → 1 (bottom); flat history → mid-line.
        val y = if (max == min) h / 2 else pad + (r - min).toDouble() / (max - min) * (h - 2 * pad)
        "${x.round1()},${y.round1()}"
    }.joinToString(" ")
}

private fun Double.round1(): String = (round(this * 10) / 10).toString()

private fun Instant?.formatted(): String =
    this?.toString()?.take(16)?.replace("T", " ") ?: "—"
