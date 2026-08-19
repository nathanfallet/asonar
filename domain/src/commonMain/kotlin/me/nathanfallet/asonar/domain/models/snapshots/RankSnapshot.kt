package me.nathanfallet.asonar.domain.models.snapshots

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * One reading of where one of our apps ranks on a keyword, at a point in time. Unlike the top-10,
 * this captures our position at any depth — including when we sit at #37 and never show in the
 * top-10. A null [rank] means the app was not found in the results at all.
 */
@Serializable
data class RankSnapshot(
    val id: Long,
    val keywordId: Long,
    val appId: Long,
    val rank: Int?, // null = not in the results
    val totalResults: Int?,
    val capturedAt: Instant,
)

/** What it takes to record a [RankSnapshot]. */
@Serializable
data class RankSnapshotPayload(
    val keywordId: Long,
    val appId: Long,
    val rank: Int?,
    val totalResults: Int?,
    val capturedAt: Instant,
)
