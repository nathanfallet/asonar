package me.nathanfallet.asonar.domain.models.keywords

import kotlinx.serialization.Serializable
import me.nathanfallet.asonar.domain.models.apps.App
import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshot
import me.nathanfallet.asonar.domain.models.snapshots.RankSnapshot
import me.nathanfallet.asonar.domain.models.snapshots.TopAppSnapshot

/**
 * The full current picture of a keyword: the term, its latest popularity, the latest top-of-results,
 * and where each of our apps latest ranks on it. Assembled from the snapshot history for reading.
 */
@Serializable
data class KeywordDetail(
    val keyword: Keyword,
    val latestPopularity: PopularitySnapshot? = null,
    val topApps: List<TopAppSnapshot> = emptyList(),
    val ranks: List<KeywordAppRank> = emptyList(),
)

/** One of our apps and its latest rank on the keyword. */
@Serializable
data class KeywordAppRank(
    val app: App,
    val rank: RankSnapshot,
)
