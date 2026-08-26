package me.nathanfallet.asonar.domain.models.keywords

import kotlinx.serialization.Serializable

/**
 * The "brain"'s read on one keyword for one of our apps: is it worth targeting? Codifies the app-aso
 * practitioner shortcut — cross **who owns the term in their title/subtitle** with **recent review
 * velocity**, weighed against *our own* velocity (if the leaders grow reviews fast but we grow faster,
 * we can still pass them). Relevance is deliberately out of scope for now: we handle it upstream, by
 * choosing which keywords we track.
 */
@Serializable
data class KeywordOpportunity(
    val keyword: Keyword,
    val verdict: OpportunityVerdict,
    val score: Int? = null,                     // 0-100, higher = better opportunity; null if unknown
    val popularity: Int? = null,                // search volume 0-100
    val ourRank: Int? = null,                   // our current rank, null = not in the scanned results
    val ourRatingsPer30d: Int? = null,          // our review velocity in this market
    val top10MedianRatingsPer30d: Int? = null,  // the leaders' median review velocity
    val velocityAdvantage: Double? = null,      // ours / theirs; > 1 means we out-grow the leaders
    val wallStrength: Double = 0.0,             // 0..1 how strongly the top is held (position × title × reviews)
    val top10TitleUsage: Double = 0.0,          // 0..1 share of the top with the term in title (display)
    val totalResults: Int? = null,
    val comment: String = "",                   // human-readable "why"
)

/** Yes / Yes-but / No / Réserve, per the app-aso opportunity matrix. */
@Serializable
enum class OpportunityVerdict {
    YES,       // the breach: nobody owns the title, or we out-velocity the leaders → go for it
    YES_BUT,   // reachable, but climb it as our authority grows
    NO,        // a wall: leaders own the title AND out-review us — not worth it short term
    RESERVE,   // relevant but too little volume to prioritise now
    UNKNOWN,   // not enough data yet (no popularity / no results captured)
}
