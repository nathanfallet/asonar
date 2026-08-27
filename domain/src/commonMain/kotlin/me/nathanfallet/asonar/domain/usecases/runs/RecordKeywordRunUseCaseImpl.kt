package me.nathanfallet.asonar.domain.usecases.runs

import me.nathanfallet.asonar.domain.models.runs.KeywordRunPayload
import me.nathanfallet.asonar.domain.models.runs.KeywordRunResult
import me.nathanfallet.asonar.domain.models.snapshots.AppRatingSnapshotPayload
import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshotPayload
import me.nathanfallet.asonar.domain.models.snapshots.RankSnapshotPayload
import me.nathanfallet.asonar.domain.models.snapshots.TopAppSnapshotPayload
import me.nathanfallet.asonar.domain.repositories.AppRatingSnapshotsRepository
import me.nathanfallet.asonar.domain.repositories.PopularitySnapshotsRepository
import me.nathanfallet.asonar.domain.repositories.RankSnapshotsRepository
import me.nathanfallet.asonar.domain.repositories.TopAppSnapshotsRepository

class RecordKeywordRunUseCaseImpl(
    private val popularitySnapshotsRepository: PopularitySnapshotsRepository,
    private val rankSnapshotsRepository: RankSnapshotsRepository,
    private val topAppSnapshotsRepository: TopAppSnapshotsRepository,
    private val appRatingSnapshotsRepository: AppRatingSnapshotsRepository,
) : RecordKeywordRunUseCase {

    override suspend fun invoke(payload: KeywordRunPayload): KeywordRunResult {
        val popularity = payload.popularity?.let { value ->
            popularitySnapshotsRepository.create(
                PopularitySnapshotPayload(payload.keywordId, value, payload.capturedAt)
            )
        }
        // Each snapshot kind is written in ONE batch insert (one statement, one transaction) instead of
        // a create()-per-row: a fetch records ~200 app-ratings + top-10 + ranks, so per-row inserts meant
        // ~215 fsync'd commits per fetch (the DB's dominant cost — see docs/database-optimization.md).
        val ranks = rankSnapshotsRepository.createAll(
            payload.ranks.map { reading ->
                RankSnapshotPayload(
                    payload.keywordId,
                    reading.appId,
                    reading.rank,
                    reading.totalResults,
                    payload.capturedAt,
                )
            }
        )
        val topApps = topAppSnapshotsRepository.createAll(
            payload.topApps.map { reading ->
                TopAppSnapshotPayload(
                    keywordId = payload.keywordId,
                    position = reading.position,
                    storeAppId = reading.storeAppId,
                    appName = reading.appName,
                    subtitle = reading.subtitle,
                    ratingCount = reading.ratingCount,
                    averageRating = reading.averageRating,
                    capturedAt = payload.capturedAt,
                )
            }
        )
        // App ratings are keyed by (store, storeAppId, country), NOT the keyword — so the history is
        // shared across every keyword the app shows up in, and the velocity uses them all.
        val appRatings = appRatingSnapshotsRepository.createAll(
            payload.appRatings.map { reading ->
                AppRatingSnapshotPayload(
                    store = payload.store,
                    storeAppId = reading.storeAppId,
                    country = payload.country,
                    name = reading.name,
                    ratingCount = reading.ratingCount,
                    averageRating = reading.averageRating,
                    capturedAt = payload.capturedAt,
                )
            }
        )
        return KeywordRunResult(popularity, ranks, topApps, appRatings)
    }

}
