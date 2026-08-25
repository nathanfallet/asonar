package me.nathanfallet.asonar.domain.models.keywords

import kotlinx.serialization.Serializable
import me.nathanfallet.asonar.domain.models.apps.App
import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshot
import me.nathanfallet.asonar.domain.models.snapshots.RankSnapshot
import me.nathanfallet.asonar.domain.models.snapshots.TopAppSnapshot

/**
 * The full current picture of a keyword: the term, its latest popularity, the latest top-of-results,
 * and where each of our apps latest ranks on it. Assembled from the snapshot history for reading.
 */
@Serializable
data class KeywordDetail(
    val keyword: Keyword,
    val latestPopularity: PopularitySnapshot? = null,
    val topApps: List<KeywordTopApp> = emptyList(),
    val ranks: List<KeywordAppRank> = emptyList(),
)

/**
 * A top-of-results app enriched with its rating velocity, pulled from the shared app-ratings history
 * (which spans every keyword this app shows up on, not just this one). [ratingsPer30d] is the ASO
 * signal that matters: how many new ratings it gains in a month — momentum, not the star average.
 */
@Serializable
data class KeywordTopApp(
    val snapshot: TopAppSnapshot,
    val ratingsPerDay: Double? = null,
) {

    /** New ratings expected over 30 days (velocity × 30). Null until the velocity is known. */
    val ratingsPer30d: Int?
        get() = ratingsPerDay?.let { kotlin.math.round(it * 30).toInt() }

}

/** One of our apps and its latest rank on the keyword. */
@Serializable
data class KeywordAppRank(
    val app: App,
    val rank: RankSnapshot,
)
