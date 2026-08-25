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
    val entries: List<KeywordCoverageEntry>,
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
