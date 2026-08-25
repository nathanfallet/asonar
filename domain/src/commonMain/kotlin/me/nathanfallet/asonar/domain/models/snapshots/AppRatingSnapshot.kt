package me.nathanfallet.asonar.domain.models.snapshots

import kotlinx.serialization.Serializable
import me.nathanfallet.asonar.domain.models.apps.Store
import kotlin.time.Instant

/**
 * One reading of an app's ratings — how many ratings it has and its average — for a given store and
 * market, at a point in time. Gathered opportunistically from the search results whenever the app
 * shows up, so its history lets us see how fast it gains ratings (and interpolate between readings).
 * Keyed by (store, storeAppId, country); append-only.
 */
@Serializable
data class AppRatingSnapshot(
    val id: Long,
    val store: Store,
    val storeAppId: String,
    val country: String,
    val name: String,
    val ratingCount: Int?,
    val averageRating: Double?,
    val capturedAt: Instant,
)

/** What it takes to record an [AppRatingSnapshot]. */
@Serializable
data class AppRatingSnapshotPayload(
    val store: Store,
    val storeAppId: String,
    val country: String,
    val name: String,
    val ratingCount: Int?,
    val averageRating: Double?,
    val capturedAt: Instant,
)
