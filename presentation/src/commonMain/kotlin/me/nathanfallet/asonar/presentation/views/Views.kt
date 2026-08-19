package me.nathanfallet.asonar.presentation.views

/** The shell data every page needs. */
data class LayoutView(
    val title: String,
)

/** The dashboard: the tracked keywords with their latest popularity. */
data class DashboardView(
    val layout: LayoutView,
    val keywords: List<KeywordRowView>,
)

/** One keyword row on the dashboard, pre-formatted so the template stays logic-free. */
data class KeywordRowView(
    val term: String,
    val store: String,
    val country: String,
    val popularityLabel: String,
    val popularityValue: Int,
    val hasPopularity: Boolean,
    val capturedAt: String,
)
