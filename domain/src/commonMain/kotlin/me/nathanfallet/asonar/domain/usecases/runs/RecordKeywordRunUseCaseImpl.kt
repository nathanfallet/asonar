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
import kotlin.time.Duration.Companion.hours

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
        // App ratings are keyed by (store, storeAppId, country), NOT the keyword — the history is shared
        // across every keyword the app shows up in. So an app that recurs across many keywords fetched
        // back-to-back would write a near-identical row each time; gate it: record a new point only when
        // the count actually moved (and it's been at least RATING_MIN_AGE), or it's been RATING_MAX_AGE
        // (so we always keep at least a daily point even when nothing moves). Keeps the history useful
        // without bloating the biggest table.
        val lastByApp = appRatingSnapshotsRepository.latestByAppIds(
            payload.store,
            payload.country,
            payload.appRatings.map { it.storeAppId },
        )
        val appRatings = appRatingSnapshotsRepository.createAll(
            payload.appRatings.filter { reading ->
                val last = lastByApp[reading.storeAppId]
                val age = last?.let { payload.capturedAt - it.capturedAt }
                when {
                    last == null || age == null -> true
                    age >= RATING_MAX_AGE -> true
                    age >= RATING_MIN_AGE && reading.ratingCount != last.ratingCount -> true
                    else -> false
                }
            }.map { reading ->
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

    companion object {
        /** Never re-record an app's rating more than once an hour, even while it's climbing. */
        private val RATING_MIN_AGE = 1.hours

        /** But always keep at least a daily point, even when the count doesn't move. */
        private val RATING_MAX_AGE = 24.hours
    }

}
