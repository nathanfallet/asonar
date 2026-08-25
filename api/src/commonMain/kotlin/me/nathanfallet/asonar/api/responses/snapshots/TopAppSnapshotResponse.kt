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
    val capturedAt: Instant,
)

/** A keyword's most recent top-of-results. */
@Serializable
data class TopAppSnapshotsResponse(
    val topApps: List<TopAppSnapshotResponse>,
)
