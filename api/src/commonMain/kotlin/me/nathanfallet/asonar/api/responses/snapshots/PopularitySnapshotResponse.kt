package me.nathanfallet.asonar.api.responses.snapshots

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** One popularity reading (0–100) at a point in time. */
@Serializable
data class PopularitySnapshotResponse(
    val id: Long,
    val keywordId: Long,
    val popularity: Int,
    val capturedAt: Instant,
)

/** A keyword's popularity history. */
@Serializable
data class PopularitySnapshotsResponse(
    val snapshots: List<PopularitySnapshotResponse>,
)
