package me.nathanfallet.asonar.presentation.routes.web

import io.ktor.http.*
import io.ktor.server.freemarker.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.nathanfallet.asonar.api.Serialization
import me.nathanfallet.asonar.domain.models.apps.*
import me.nathanfallet.asonar.domain.models.keywords.CandidateStatus
import me.nathanfallet.asonar.domain.models.keywords.KeywordCandidate
import me.nathanfallet.asonar.domain.models.keywords.KeywordOpportunity
import me.nathanfallet.asonar.domain.models.keywords.OpportunityVerdict
import me.nathanfallet.asonar.domain.usecases.apps.GetAppKeywordCoverageUseCase
import me.nathanfallet.asonar.domain.usecases.apps.GetAppUseCase
import me.nathanfallet.asonar.domain.usecases.apps.ListAppsUseCase
import me.nathanfallet.asonar.domain.usecases.apps.RefreshAppKeywordsUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.DiscoverKeywordCandidatesUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.GetKeywordOpportunitiesUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.ListKeywordCandidatesUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.ReviewKeywordCandidatesUseCase
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
    val getAppUseCase: GetAppUseCase,
    val discoverKeywordCandidatesUseCase: DiscoverKeywordCandidatesUseCase,
    val listKeywordCandidatesUseCase: ListKeywordCandidatesUseCase,
    val reviewKeywordCandidatesUseCase: ReviewKeywordCandidatesUseCase,
)

/** The "Apps" tab: pick an app, then see its recommendations + ranking coverage (stats + chart + table). */
fun Route.appsWebRoutes(dependencies: AppsWebRoutesDependencies) = with(dependencies) {
    get("/apps") {
        // Ours first, competitors after: the list is a launcher for our own optimization work, the
        // watched apps are context.
        val apps = listAppsUseCase()
            .sortedBy { it.role.ordinal }
            .map {
                AppOptionView(
                    it.id,
                    it.name,
                    it.store.name,
                    it.storeAppId,
                    it.role.label(),
                    it.role.cssClass(),
                )
            }
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

    // --- Discovery: propose terms, then take or bury them (see DiscoverKeywordCandidatesUseCase) ---

    get("/apps/{id}/candidates") {
        val id = call.parameters["id"]?.toLongOrNull()
        val app = id?.let { getAppUseCase(it) }
        if (app == null) {
            call.respond(HttpStatusCode.NotFound, "App not found")
            return@get
        }
        val pending = listKeywordCandidatesUseCase(app.id, setOf(CandidateStatus.NEW)).orEmpty()
        val added = listKeywordCandidatesUseCase(app.id, setOf(CandidateStatus.ADDED)).orEmpty()
        val dismissed = listKeywordCandidatesUseCase(app.id, setOf(CandidateStatus.DISMISSED)).orEmpty()
        call.respond(
            FreeMarkerContent(
                "candidates.ftl",
                mapOf(
                    "view" to AppCandidatesView(
                        layout = LayoutView("${app.name} · Découverte", "apps"),
                        appId = app.id,
                        appName = app.name,
                        newCount = pending.size,
                        addedCount = added.size,
                        dismissedCount = dismissed.size,
                        countriesValue = pending.map { it.country }.distinct().sorted().joinToString(", "),
                        rows = pending.map { it.toCandidateRow() },
                    )
                ),
            )
        )
    }
    post("/apps/{id}/candidates/discover") {
        val id = call.parameters["id"]?.toLongOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.NotFound, "App not found")
            return@post
        }
        val params = call.receiveParameters()
        val countries = params["countries"].splitList().map { it.uppercase() }
        val seeds = params["seeds"].splitList().map { it.lowercase() }
        // Fire-and-forget, like the coverage auto-refresh: a pass drives a real browser, one request
        // per seed and per market, so it takes minutes — far too long to hold an HTML form open.
        call.application.launch {
            discoverKeywordCandidatesUseCase(id, countries.ifEmpty { null }, seeds.ifEmpty { null })
        }
        call.respondRedirect("/apps/$id/candidates")
    }
    post("/apps/{id}/candidates/review") {
        val id = call.parameters["id"]?.toLongOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.NotFound, "App not found")
            return@post
        }
        val params = call.receiveParameters()
        val ids = params.getAll("candidate").orEmpty().mapNotNull { it.toLongOrNull() }
        // Two submit buttons, one checkbox list: the button that was pressed says what to do.
        when (params["action"]) {
            "accept" -> reviewKeywordCandidatesUseCase.accept(ids)
            "dismiss" -> reviewKeywordCandidatesUseCase.dismiss(ids)
        }
        call.respondRedirect("/apps/$id/candidates")
    }
}

/** Reads a comma/space separated form field into a clean list. */
private fun String?.splitList(): List<String> =
    orEmpty().split(",", " ").map { it.trim() }.filter { it.isNotEmpty() }.distinct()

private fun KeywordCandidate.toCandidateRow() = CandidateRowView(
    id = id,
    term = term,
    country = country,
    popularityLabel = popularity?.toString() ?: "—",
    // Unknown popularity sorts below everything measured, rather than above it as a 0 would.
    popularitySort = popularity ?: -1,
    atFloor = (popularity ?: Int.MAX_VALUE) <= POPULARITY_FLOOR,
    sources = sources.map { it.name }.sorted(),
    detail = detail.orEmpty(),
)

/**
 * Apple's search index bottoms out at 5 — a term there is essentially never searched, however winnable
 * it looks. Flagged in the review list so the floor is obvious at a glance.
 */
private const val POPULARITY_FLOOR = 5

// --- Chart wire format (embedded as JSON, rendered client-side by /js/chart.js) ---

// Generic wire format for /js/chart.js (a reusable multi-line time chart). Only `yInvert`, `series`
// and each series' `label`/`color`/`points` are required; the rest is optional so the same component
// serves other charts (e.g. a keyword's popularity over time) by declaring different filters.
@Serializable
private data class ChartData(
    val yInvert: Boolean,
    val series: List<ChartSeries>,
    val filters: ChartFilters? = null,
)

/** Which filter controls the chart shows. Empty/false → the control is hidden. */
@Serializable
private data class ChartFilters(
    val country: Boolean = false,   // a "country" selector, built from each series' `country`
    val period: List<Int> = emptyList(), // day-windows for the period segmented control (e.g. 1, 7, 30)
    val top: List<Int> = emptyList(),    // rank thresholds for the top-N segmented control (rank charts)
)

@Serializable
private data class ChartSeries(
    val label: String,
    val color: String,
    val points: List<ChartPoint>,
    val country: String? = null,    // for the country filter
    val badge: String? = null,      // shown after the label in the legend (e.g. the current rank)
)

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
        // "term · COUNTRY" so the same term across storefronts stays distinguishable in the legend + tooltip;
        // `country` drives the country filter, `badge` shows the current rank right in the legend.
        ChartSeries(
            label = "${entry.keyword.term} · ${entry.keyword.country}",
            color = PALETTE[index % PALETTE.size],
            points = points,
            country = entry.keyword.country,
            badge = entry.rank?.let { "#$it" },
        )
    }
    val chartData = ChartData(
        yInvert = true,
        series = series,
        filters = ChartFilters(country = true, period = listOf(1, 7, 30), top = listOf(5, 25, 100)),
    )

    return AppCoverageView(
        layout = LayoutView(app.name, "apps"),
        appId = app.id,
        appName = app.name,
        store = app.store.name,
        storeAppId = app.storeAppId,
        roleLabel = app.role.label(),
        roleClass = app.role.cssClass(),
        rankedCount = summary.rankedCount,
        totalCount = summary.trackedCount,
        summary = summary.toSummaryView(),
        chartJson = Serialization.json.encodeToString(chartData),
        hasChart = series.isNotEmpty(),
        recommendations = opportunities.map { it.toRecommendationRow() },
        rows = entries.map { it.toRow() },
    )
}

/** French label for a role — the web UI is in French, the wire format keeps the enum name. */
private fun AppRole.label() = when (this) {
    AppRole.OWNED -> "À nous"
    AppRole.COMPETITOR -> "Concurrent"
}

private fun AppRole.cssClass() = when (this) {
    AppRole.OWNED -> "owned"
    AppRole.COMPETITOR -> "competitor"
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
