package me.nathanfallet.asonar.api.responses.snapshots

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** One row of a keyword's top-of-results at a point in time. */
@Serializable
data class TopAppSnapshotResponse(
    val id: Long,
    val keywordId: Long,
    val position: Int,
    val storeAppId: String,
    val appName: String,
    val ratingCount: Int? = null,
    val averageRating: Double? = null,
    /** New ratings expected over 30 days (rating velocity × 30). Null until there's enough history. */
    val ratingsPer30d: Int? = null,
    val capturedAt: Instant,
)

/** A keyword's most recent top-of-results. */
@Serializable
data class TopAppSnapshotsResponse(
    val topApps: List<TopAppSnapshotResponse>,
)
