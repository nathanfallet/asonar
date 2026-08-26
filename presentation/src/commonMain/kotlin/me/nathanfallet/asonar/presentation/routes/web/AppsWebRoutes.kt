package me.nathanfallet.asonar.presentation.routes.web

import io.ktor.http.*
import io.ktor.server.freemarker.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.nathanfallet.asonar.api.Serialization
import me.nathanfallet.asonar.domain.models.apps.AppKeywordCoverage
import me.nathanfallet.asonar.domain.models.apps.CoverageSummary
import me.nathanfallet.asonar.domain.models.apps.KeywordCoverageEntry
import me.nathanfallet.asonar.domain.models.apps.RankPoint
import me.nathanfallet.asonar.domain.models.keywords.KeywordOpportunity
import me.nathanfallet.asonar.domain.models.keywords.OpportunityVerdict
import me.nathanfallet.asonar.domain.usecases.apps.GetAppKeywordCoverageUseCase
import me.nathanfallet.asonar.domain.usecases.apps.ListAppsUseCase
import me.nathanfallet.asonar.domain.usecases.apps.RefreshAppKeywordsUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.GetKeywordOpportunitiesUseCase
import me.nathanfallet.asonar.presentation.views.*
import kotlin.math.max
import kotlin.math.round
import kotlin.time.Instant

/** Use cases the apps tab needs. */
data class AppsWebRoutesDependencies(
    val listAppsUseCase: ListAppsUseCase,
    val getAppKeywordCoverageUseCase: GetAppKeywordCoverageUseCase,
    val getKeywordOpportunitiesUseCase: GetKeywordOpportunitiesUseCase,
    val refreshAppKeywordsUseCase: RefreshAppKeywordsUseCase,
)

/** The "Apps" tab: pick an app, then see its recommendations + ranking coverage (stats + chart + table). */
fun Route.appsWebRoutes(dependencies: AppsWebRoutesDependencies) = with(dependencies) {
    get("/apps") {
        val apps = listAppsUseCase().map { AppOptionView(it.id, it.name, it.store.name, it.storeAppId) }
        call.respond(
            FreeMarkerContent("apps.ftl", mapOf("view" to AppsListView(LayoutView("Apps", "apps"), apps))),
        )
    }
    get("/apps/{id}") {
        val id = call.parameters["id"]?.toLongOrNull()
        val coverage = id?.let { getAppKeywordCoverageUseCase(it) }
        if (coverage == null) {
            call.respond(HttpStatusCode.NotFound, "App not found")
            return@get
        }
        val opportunities = getKeywordOpportunitiesUseCase(id).orEmpty()
        // Fire-and-forget: refresh the relevant keywords in the background so the next view is fresher.
        call.application.launch { refreshAppKeywordsUseCase(id) }
        call.respond(FreeMarkerContent("app.ftl", mapOf("view" to coverage.toCoverageView(opportunities))))
    }
}

// --- Chart wire format (embedded as JSON, rendered client-side by /js/chart.js) ---

@Serializable
private data class ChartData(val yInvert: Boolean, val series: List<ChartSeries>)

@Serializable
private data class ChartSeries(val label: String, val color: String, val points: List<ChartPoint>)

@Serializable
private data class ChartPoint(val t: Long, val v: Int)

private val PALETTE = listOf(
    "#3ddc97", "#5aa9ff", "#f6c453", "#ff6b6b", "#b98bff",
    "#48d1cc", "#ff9f43", "#a0e57c", "#ff7ab6", "#7ce0d3",
)

private fun AppKeywordCoverage.toCoverageView(opportunities: List<KeywordOpportunity>): AppCoverageView {
    // One coloured line per keyword that has at least one ranked reading, over a shared time axis.
    val series = entries.mapNotNull { entry ->
        val points =
            entry.history.mapNotNull { p -> p.rank?.let { ChartPoint(p.capturedAt.toEpochMilliseconds(), it) } }
        if (points.isEmpty()) null else entry to points
    }.mapIndexed { index, (entry, points) ->
        // "term · COUNTRY" so the same term across storefronts stays distinguishable in the legend + tooltip.
        ChartSeries("${entry.keyword.term} · ${entry.keyword.country}", PALETTE[index % PALETTE.size], points)
    }
    val chartData = ChartData(yInvert = true, series = series)

    return AppCoverageView(
        layout = LayoutView(app.name, "apps"),
        appName = app.name,
        store = app.store.name,
        storeAppId = app.storeAppId,
        rankedCount = summary.rankedCount,
        totalCount = summary.trackedCount,
        summary = summary.toSummaryView(),
        chartJson = Serialization.json.encodeToString(chartData),
        hasChart = series.isNotEmpty(),
        recommendations = opportunities.map { it.toRecommendationRow() },
        rows = entries.map { it.toRow() },
    )
}

private fun KeywordOpportunity.toRecommendationRow(): RecommendationRowView {
    val (label, css, order) = when (verdict) {
        OpportunityVerdict.YES -> Triple("Yes", "yes", 0)
        OpportunityVerdict.YES_BUT -> Triple("Yes but", "yesbut", 1)
        OpportunityVerdict.RESERVE -> Triple("Réserve", "reserve", 2)
        OpportunityVerdict.NO -> Triple("No", "no", 3)
        OpportunityVerdict.UNKNOWN -> Triple("?", "unknown", 4)
    }
    return RecommendationRowView(
        keywordId = keyword.id,
        term = keyword.term,
        country = keyword.country,
        verdictLabel = label,
        verdictClass = css,
        verdictOrder = order,
        scoreLabel = score?.toString() ?: "—",
        comment = comment,
    )
}

private fun CoverageSummary.toSummaryView() = CoverageSummaryView(
    avgRankLabel = averageRank?.let { "#$it" } ?: "—",
    bestRankLabel = bestRank?.let { "#$it" } ?: "—",
    worstRankLabel = worstRank?.let { "#$it" } ?: "—",
    top5 = top5,
    top25 = top25,
    top100 = top100,
    beyond100 = beyond100,
    distMax = max(1, listOf(top5, top25, top100, beyond100).max()),
    wentUp = wentUp,
    wentDown = wentDown,
    unchanged = unchanged,
    moveTotal = max(1, wentUp + wentDown + unchanged),
)

private fun KeywordCoverageEntry.toRow() = CoverageRowView(
    keywordId = keyword.id,
    term = keyword.term,
    country = keyword.country,
    popularityLabel = popularity?.toString() ?: "—",
    rankLabel = rank?.let { "#$it" } ?: "—",
    rankSort = rank ?: 99_999,
    ranked = rank != null,
    sparkPoints = sparkline(history),
    capturedAt = capturedAt.formatted(),
)

/**
 * A per-row SVG polyline (points for a `0 0 100 24` viewBox) of the rank over time — a quick trend
 * glance right in the table, without hunting for the line in the big chart. Only ranked readings are
 * plotted; a better (lower) rank sits higher. "" with fewer than two points.
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
        val y = if (max == min) h / 2 else pad + (r - min).toDouble() / (max - min) * (h - 2 * pad)
        "${(round(x * 10) / 10)},${(round(y * 10) / 10)}"
    }.joinToString(" ")
}

private fun Instant?.formatted(): String =
    this?.toString()?.take(16)?.replace("T", " ") ?: "—"
