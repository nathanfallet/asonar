package me.nathanfallet.asonar.presentation.mappers.apps

import me.nathanfallet.asonar.api.responses.apps.AppRatingHistoryResponse
import me.nathanfallet.asonar.api.responses.apps.AppRatingSnapshotResponse
import me.nathanfallet.asonar.domain.models.snapshots.AppRatingHistory
import me.nathanfallet.asonar.domain.models.snapshots.AppRatingSnapshot

fun AppRatingSnapshot.toAppRatingSnapshotResponse() = AppRatingSnapshotResponse(
    id = id,
    store = store.name,
    storeAppId = storeAppId,
    country = country,
    name = name,
    ratingCount = ratingCount,
    averageRating = averageRating,
    capturedAt = capturedAt,
)

fun AppRatingHistory.toAppRatingHistoryResponse() = AppRatingHistoryResponse(
    store = store.name,
    storeAppId = storeAppId,
    country = country,
    name = name,
    latestRatingCount = latestRatingCount,
    latestAverageRating = latestAverageRating,
    ratingsPer30d = ratingsPer30d,
    snapshots = snapshots.map { it.toAppRatingSnapshotResponse() },
)
