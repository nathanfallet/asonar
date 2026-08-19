package me.nathanfallet.asonar.api.responses.keywords

import kotlinx.serialization.Serializable
import me.nathanfallet.asonar.api.responses.apps.AppResponse
import me.nathanfallet.asonar.api.responses.snapshots.RankSnapshotResponse
import me.nathanfallet.asonar.api.responses.snapshots.TopAppSnapshotResponse

/** The full current picture of a keyword: its data, the top-of-results and our apps' ranks. */
@Serializable
data class KeywordDetailResponse(
    val keyword: KeywordResponse,
    val topApps: List<TopAppSnapshotResponse>,
    val ranks: List<KeywordAppRankResponse>,
)

/** One of our apps and its latest rank on the keyword. */
@Serializable
data class KeywordAppRankResponse(
    val app: AppResponse,
    val rank: RankSnapshotResponse,
)
