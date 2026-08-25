package me.nathanfallet.asonar.domain.models.snapshots

import kotlinx.serialization.Serializable
import me.nathanfallet.asonar.domain.models.apps.Store

/**
 * An app's ratings history for a store and market, plus the derived velocity — how many ratings it
 * gains per day (least-squares slope over the readings). With the slope and the latest reading you
 * can interpolate the count at any date, which is data we otherwise don't have.
 */
@Serializable
data class AppRatingHistory(
    val store: Store,
    val storeAppId: String,
    val country: String,
    val name: String? = null,
    val latestRatingCount: Int? = null,
    val latestAverageRating: Double? = null,
    val ratingsPerDay: Double? = null,
    val snapshots: List<AppRatingSnapshot> = emptyList(),
)
