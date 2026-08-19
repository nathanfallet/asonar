package me.nathanfallet.asonar.presentation.mappers.snapshots

import me.nathanfallet.asonar.api.responses.snapshots.PopularitySnapshotResponse
import me.nathanfallet.asonar.api.responses.snapshots.RankSnapshotResponse
import me.nathanfallet.asonar.api.responses.snapshots.TopAppSnapshotResponse
import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshot
import me.nathanfallet.asonar.domain.models.snapshots.RankSnapshot
import me.nathanfallet.asonar.domain.models.snapshots.TopAppSnapshot

fun PopularitySnapshot.toPopularitySnapshotResponse() = PopularitySnapshotResponse(
    id = id,
    keywordId = keywordId,
    popularity = popularity,
    capturedAt = capturedAt,
)

fun RankSnapshot.toRankSnapshotResponse() = RankSnapshotResponse(
    id = id,
    keywordId = keywordId,
    appId = appId,
    rank = rank,
    totalResults = totalResults,
    capturedAt = capturedAt,
)

fun TopAppSnapshot.toTopAppSnapshotResponse() = TopAppSnapshotResponse(
    id = id,
    keywordId = keywordId,
    position = position,
    storeAppId = storeAppId,
    appName = appName,
    capturedAt = capturedAt,
)
