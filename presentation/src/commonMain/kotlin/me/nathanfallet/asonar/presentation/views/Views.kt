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
)

/** One app's ranking coverage across every tracked keyword on its store. */
data class AppCoverageView(
    val layout: LayoutView,
    val appName: String,
    val store: String,
    val storeAppId: String,
    val rankedCount: Int,
    val totalCount: Int,
    val rows: List<CoverageRowView>,
)

data class CoverageRowView(
    val keywordId: Long,
    val term: String,
    val country: String,
    val popularityLabel: String,
    val rankLabel: String,
    val ranked: Boolean,
    val sparkPoints: String, // SVG polyline points, "" when there isn't enough history
    val capturedAt: String,
)

/** The MCP connection guide. */
data class McpGuideView(
    val layout: LayoutView,
    val mcpUrl: String,
    val claudeCodeCommand: String,
)
