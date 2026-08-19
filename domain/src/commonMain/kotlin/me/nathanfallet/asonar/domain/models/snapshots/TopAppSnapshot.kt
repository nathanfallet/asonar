package me.nathanfallet.asonar.domain.models.snapshots

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * One row of the top-of-results for a keyword at a point in time — competitive intelligence on who
 * owns the term. The rows of a single observation share the same [capturedAt], so a run's top-10 is
 * read back by grouping on it. The app is stored raw (store id + name); matching it to one of our
 * [me.nathanfallet.asonar.domain.models.apps.App]s is a query, not a foreign key.
 */
@Serializable
data class TopAppSnapshot(
    val id: Long,
    val keywordId: Long,
    val position: Int, // 1-based rank within the captured results
    val storeAppId: String,
    val appName: String,
    val capturedAt: Instant,
)

/** What it takes to record a [TopAppSnapshot]. */
@Serializable
data class TopAppSnapshotPayload(
    val keywordId: Long,
    val position: Int,
    val storeAppId: String,
    val appName: String,
    val capturedAt: Instant,
)
