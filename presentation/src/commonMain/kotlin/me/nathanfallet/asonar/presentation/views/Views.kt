package me.nathanfallet.asonar.presentation.views

/** The shell data every page needs. [activeNav] highlights the current nav item. */
data class LayoutView(
    val title: String,
    val activeNav: String,
)

/** The dashboard: the tracked keywords with their latest popularity. */
data class DashboardView(
    val layout: LayoutView,
    val keywords: List<KeywordRowView>,
)

/** One keyword row on the dashboard, pre-formatted so the template stays logic-free. */
data class KeywordRowView(
    val id: Long,
    val term: String,
    val store: String,
    val country: String,
    val popularityLabel: String,
    val popularityValue: Int,
    val hasPopularity: Boolean,
    val capturedAt: String,
)

/** A keyword's detail page: its data, the top-of-results and our apps' ranks. */
data class KeywordDetailView(
    val layout: LayoutView,
    val id: Long,
    val term: String,
    val store: String,
    val country: String,
    val popularityLabel: String,
    val popularityValue: Int,
    val hasPopularity: Boolean,
    val capturedAt: String,
    val topApps: List<TopAppRowView>,
    val ranks: List<RankRowView>,
)

data class TopAppRowView(
    val position: Int,
    val appName: String,
    val subtitle: String,
    val storeAppId: String,
    val ratings: String,
    val averageRating: String,
    val reviews30d: String,
)

data class RankRowView(
    val appName: String,
    val rankLabel: String,
    val totalResults: String,
    val capturedAt: String,
)

/**
 * An app's discovery page: the terms sources proposed, waiting to be taken or buried. Everything is
 * pre-formatted so the template stays logic-free, like the other views.
 */
data class AppCandidatesView(
    val layout: LayoutView,
    val appId: Long,
    val appName: String,
    val newCount: Int,
    val addedCount: Int,
    val dismissedCount: Int,
    val countriesValue: String,   // pre-filled markets for the discovery form
    val rows: List<CandidateRowView>,
)

data class CandidateRowView(
    val id: Long,
    val term: String,
    val country: String,
    val popularityLabel: String,
    val popularitySort: Int,      // for the sortable table: unknown sinks to the bottom
    val atFloor: Boolean,         // popularity <= 5: the floor of Apple's index, nobody searches it
    val sources: List<String>,
    val detail: String,
)

/** The apps tab: pick one of our tracked apps to see its keyword coverage. */
data class AppsListView(
    val layout: LayoutView,
    val apps: List<AppOptionView>,
)

data class AppOptionView(
    val id: Long,
    val name: String,
    val store: String,
    val storeAppId: String,
    val roleLabel: String,
    val roleClass: String,
)

/** One app's ranking coverage: stats + the multi-line rank chart + the per-keyword table. */
data class AppCoverageView(
    val layout: LayoutView,
    val appId: Long,
    val appName: String,
    val store: String,
    val storeAppId: String,
    val roleLabel: String,
    val roleClass: String,
    val rankedCount: Int,
    val totalCount: Int,
    val summary: CoverageSummaryView,
    val chartJson: String, // JSON consumed by /js/chart.js
    val hasChart: Boolean,
    val recommendations: List<RecommendationRowView>,
    val rows: List<CoverageRowView>,
)

/** One scored keyword in the recommendations card: verdict + score + the "why". */
data class RecommendationRowView(
    val keywordId: Long,
    val term: String,
    val country: String,
    val verdictLabel: String,
    val verdictClass: String, // css modifier: yes / yesbut / no / reserve / unknown
    val verdictOrder: Int,    // sort key: YES=0 … UNKNOWN=4
    val scoreLabel: String,
    val comment: String,
)

/** AppFigures-style summary cards above the chart. */
data class CoverageSummaryView(
    val avgRankLabel: String,
    val bestRankLabel: String,
    val worstRankLabel: String,
    val top5: Int,
    val top25: Int,
    val top100: Int,
    val beyond100: Int,
    val distMax: Int,
    val wentUp: Int,
    val wentDown: Int,
    val unchanged: Int,
    val moveTotal: Int,
)

data class CoverageRowView(
    val keywordId: Long,
    val term: String,
    val country: String,
    val popularityLabel: String,
    val rankLabel: String,
    val rankSort: Int,       // sort key: the rank, or a large number when not ranked
    val ranked: Boolean,
    val sparkPoints: String, // SVG polyline points, "" when there isn't enough history
    val capturedAt: String,
)

/** The MCP connection guide. [tools] is generated live from the registered MCP tools. */
data class McpGuideView(
    val layout: LayoutView,
    val mcpUrl: String,
    val claudeCodeCommand: String,
    val tools: List<ToolInfoView>,
)

data class ToolInfoView(
    val name: String,
    val description: String,
)
