package me.nathanfallet.asonar.domain.models.apps

import kotlinx.serialization.Serializable
import me.nathanfallet.asonar.domain.models.keywords.Keyword
import kotlin.time.Instant

/**
 * Where one of our apps stands across **every keyword we track on its store** — the ASO coverage
 * view. Answers "on which keywords do I rank / not rank, where, and how is it moving?" in one shot,
 * instead of opening each keyword. Built from the shared rank-snapshot history.
 */
@Serializable
data class AppKeywordCoverage(
    val app: App,
    val summary: CoverageSummary,
    val entries: List<KeywordCoverageEntry>,
)

/**
 * High-level stats over the app's coverage, à la AppFigures: the average/best/worst current rank, the
 * distribution across rank bands, and how many keywords moved up/down/held since the previous reading.
 */
@Serializable
data class CoverageSummary(
    val averageRank: Int? = null,
    val bestRank: Int? = null,
    val worstRank: Int? = null,
    val rankedCount: Int = 0,
    val trackedCount: Int = 0,
    // Exclusive rank bands over the *ranked* keywords.
    val top5: Int = 0,
    val top25: Int = 0,
    val top100: Int = 0,
    val beyond100: Int = 0,
    // Movement vs the previous reading (only keywords with a comparable prior rank are counted).
    val wentUp: Int = 0,
    val wentDown: Int = 0,
    val unchanged: Int = 0,
)

/** One tracked keyword and our app's standing on it: current rank (null = not ranked) + history. */
@Serializable
data class KeywordCoverageEntry(
    val keyword: Keyword,
    val popularity: Int? = null,
    val rank: Int? = null,
    val totalResults: Int? = null,
    val capturedAt: Instant? = null,
    val history: List<RankPoint> = emptyList(),
)

/** One point of the rank history, chronological. A null [rank] means not ranked at that time. */
@Serializable
data class RankPoint(
    val rank: Int?,
    val capturedAt: Instant,
)
