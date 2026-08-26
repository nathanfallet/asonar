package me.nathanfallet.asonar.domain.models.keywords

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * The expensive-to-compute, keyword-level signals the opportunity score needs — captured once per
 * fetch (in the background) so scoring at read time stays cheap (Option B). We store the top-of-
 * results **per competitor** (position + whether they use the term + their review strength) rather
 * than a flat aggregate, so the scorer can weigh *who* holds the top spots: a #1 that doesn't use the
 * term, or uses it with few reviews, is a weakly-held position we can pass. Everything here is
 * app-independent; our own rank/velocity are joined in when we score.
 */
@Serializable
data class KeywordSignals(
    val id: Long,
    val keywordId: Long,
    val competitors: List<CompetitorSignal>,
    val totalResults: Int? = null,
    val capturedAt: Instant,
)

/** One top-of-results app's scoring inputs at capture time. */
@Serializable
data class CompetitorSignal(
    val position: Int,               // 1-based
    val titleFactor: Double,         // 1.0 = term in title, 0.5 = subtitle only, 0.0 = not used
    val ratingCount: Int? = null,    // total ratings (a proxy for strength)
    val ratingsPer30d: Int? = null,  // recent review velocity (preferred when available)
)

/** What it takes to record a [KeywordSignals] reading. */
@Serializable
data class KeywordSignalsPayload(
    val keywordId: Long,
    val competitors: List<CompetitorSignal>,
    val totalResults: Int? = null,
    val capturedAt: Instant,
)
