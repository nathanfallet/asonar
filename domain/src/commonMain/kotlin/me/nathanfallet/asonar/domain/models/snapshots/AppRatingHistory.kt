package me.nathanfallet.asonar.domain.models.snapshots

import kotlinx.serialization.Serializable
import me.nathanfallet.asonar.domain.models.apps.Store

/**
 * An app's ratings history for a store and market, plus the derived velocity: [ratingsPer30d], the
 * ratings gained over the last 30 days (capped to the observed window — never extrapolated past the
 * data we have). Null until the readings cover at least two distinct calendar days.
 */
@Serializable
data class AppRatingHistory(
    val store: Store,
    val storeAppId: String,
    val country: String,
    val name: String? = null,
    val latestRatingCount: Int? = null,
    val latestAverageRating: Double? = null,
    val ratingsPer30d: Int? = null,
    val snapshots: List<AppRatingSnapshot> = emptyList(),
)
