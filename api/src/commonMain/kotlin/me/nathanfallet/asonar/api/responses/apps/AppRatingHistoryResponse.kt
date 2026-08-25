package me.nathanfallet.asonar.api.responses.apps

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** One reading of an app's ratings (count + average) in a market at a point in time. */
@Serializable
data class AppRatingSnapshotResponse(
    val id: Long,
    val store: String,
    val storeAppId: String,
    val country: String,
    val name: String,
    val ratingCount: Int? = null,
    val averageRating: Double? = null,
    val capturedAt: Instant,
)

/**
 * An app's ratings history in a market, with the derived velocity ([ratingsPerDay]) — how many
 * ratings it gains per day. With the slope + the latest count you can interpolate at any date.
 */
@Serializable
data class AppRatingHistoryResponse(
    val store: String,
    val storeAppId: String,
    val country: String,
    val name: String? = null,
    val latestRatingCount: Int? = null,
    val latestAverageRating: Double? = null,
    val ratingsPerDay: Double? = null,
    val snapshots: List<AppRatingSnapshotResponse> = emptyList(),
)
