package me.nathanfallet.asonar.domain.models.runs

import kotlinx.serialization.Serializable
import me.nathanfallet.asonar.domain.models.apps.Store
import kotlin.time.Instant

/**
 * Everything one fetch produced for a single keyword: its popularity, where each of our apps ranked,
 * the top-of-results, and the ratings of the apps seen — all sharing one [capturedAt] so the readings
 * line up in time. [store] and [country] key the app-rating snapshots. Whatever a run did not observe
 * is simply left out (null popularity, empty lists), and nothing of that kind is recorded.
 */
@Serializable
data class KeywordRunPayload(
    val keywordId: Long,
    val store: Store,
    val country: String,
    val capturedAt: Instant,
    val popularity: Int? = null,
    val ranks: List<AppRankReading> = emptyList(),
    val topApps: List<TopAppReading> = emptyList(),
    val appRatings: List<AppRatingReading> = emptyList(),
)

/** Where one of our apps ranked on the keyword during the run. A null [rank] means not found. */
@Serializable
data class AppRankReading(
    val appId: Long,
    val rank: Int? = null,
    val totalResults: Int? = null,
)

/** One row of the keyword's top-of-results during the run, with the app's ratings at that moment. */
@Serializable
data class TopAppReading(
    val position: Int,
    val storeAppId: String,
    val appName: String,
    val subtitle: String? = null,
    val ratingCount: Int? = null,
    val averageRating: Double? = null,
)

/** One app's ratings seen during the run — becomes an app-rating snapshot (store/country from the run). */
@Serializable
data class AppRatingReading(
    val storeAppId: String,
    val name: String,
    val ratingCount: Int? = null,
    val averageRating: Double? = null,
)
