package me.nathanfallet.asonar.domain.models.snapshots

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * One reading of a keyword's popularity (the 0–100 search-volume index) at a point in time.
 * Popularity is a property of the keyword's market, not of any app, so it hangs off the keyword
 * alone. Append-only: a new reading is a new row, so the history is never lost.
 */
@Serializable
data class PopularitySnapshot(
    val id: Long,
    val keywordId: Long,
    val popularity: Int, // 0..100
    val capturedAt: Instant,
)

/** What it takes to record a [PopularitySnapshot]. */
@Serializable
data class PopularitySnapshotPayload(
    val keywordId: Long,
    val popularity: Int,
    val capturedAt: Instant,
)
