package me.nathanfallet.asonar.api.responses.snapshots

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** One rank reading of an app on a keyword at a point in time (null [rank] = not in results). */
@Serializable
data class RankSnapshotResponse(
    val id: Long,
    val keywordId: Long,
    val appId: Long,
    val rank: Int?,
    val totalResults: Int?,
    val capturedAt: Instant,
)

/** An app's rank history on a keyword. */
@Serializable
data class RankSnapshotsResponse(
    val snapshots: List<RankSnapshotResponse>,
)
