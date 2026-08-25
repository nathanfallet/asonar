package me.nathanfallet.asonar.api.responses.apps

import kotlinx.serialization.Serializable
import me.nathanfallet.asonar.api.responses.keywords.KeywordResponse
import kotlin.time.Instant

/** One point of an app's rank history on a keyword. A null [rank] means not ranked at that time. */
@Serializable
data class RankPointResponse(
    val rank: Int? = null,
    val capturedAt: Instant,
)

/** One tracked keyword and our app's standing on it: current rank (null = not ranked) + history. */
@Serializable
data class KeywordCoverageEntryResponse(
    val keyword: KeywordResponse,
    val popularity: Int? = null,
    val rank: Int? = null,
    val totalResults: Int? = null,
    val capturedAt: Instant? = null,
    val history: List<RankPointResponse> = emptyList(),
)

/** High-level stats over an app's coverage (average/best/worst rank, distribution, movement). */
@Serializable
data class CoverageSummaryResponse(
    val averageRank: Int? = null,
    val bestRank: Int? = null,
    val worstRank: Int? = null,
    val rankedCount: Int = 0,
    val trackedCount: Int = 0,
    val top5: Int = 0,
    val top25: Int = 0,
    val top100: Int = 0,
    val beyond100: Int = 0,
    val wentUp: Int = 0,
    val wentDown: Int = 0,
    val unchanged: Int = 0,
)

/** An app's ranking coverage across every keyword tracked on its store. */
@Serializable
data class AppKeywordCoverageResponse(
    val app: AppResponse,
    val summary: CoverageSummaryResponse,
    val entries: List<KeywordCoverageEntryResponse>,
)
