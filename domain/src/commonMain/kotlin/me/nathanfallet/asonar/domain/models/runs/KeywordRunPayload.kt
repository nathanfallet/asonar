package me.nathanfallet.asonar.domain.models.runs

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Everything one fetch produced for a single keyword: its popularity, where each of our apps ranked,
 * and the top-of-results — all sharing one [capturedAt] so the readings line up in time. Whatever a
 * run did not observe is simply left out (null popularity, empty lists), and nothing of that kind is
 * recorded. The [capturedAt] is supplied by the caller so that a batch spanning many keywords can
 * stamp them all with one coherent run timestamp.
 */
@Serializable
data class KeywordRunPayload(
    val keywordId: Long,
    val capturedAt: Instant,
    val popularity: Int? = null,
    val ranks: List<AppRankReading> = emptyList(),
    val topApps: List<TopAppReading> = emptyList(),
)

/** Where one of our apps ranked on the keyword during the run. A null [rank] means not found. */
@Serializable
data class AppRankReading(
    val appId: Long,
    val rank: Int? = null,
    val totalResults: Int? = null,
)

/** One row of the keyword's top-of-results during the run. */
@Serializable
data class TopAppReading(
    val position: Int,
    val storeAppId: String,
    val appName: String,
)
