package me.nathanfallet.asonar.domain.usecases.runs

import me.nathanfallet.asonar.domain.models.runs.KeywordRunPayload
import me.nathanfallet.asonar.domain.models.runs.KeywordRunResult
import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshotPayload
import me.nathanfallet.asonar.domain.models.snapshots.RankSnapshotPayload
import me.nathanfallet.asonar.domain.models.snapshots.TopAppSnapshotPayload
import me.nathanfallet.asonar.domain.repositories.PopularitySnapshotsRepository
import me.nathanfallet.asonar.domain.repositories.RankSnapshotsRepository
import me.nathanfallet.asonar.domain.repositories.TopAppSnapshotsRepository

class RecordKeywordRunUseCaseImpl(
    private val popularitySnapshotsRepository: PopularitySnapshotsRepository,
    private val rankSnapshotsRepository: RankSnapshotsRepository,
    private val topAppSnapshotsRepository: TopAppSnapshotsRepository,
) : RecordKeywordRunUseCase {

    override suspend fun invoke(payload: KeywordRunPayload): KeywordRunResult {
        val popularity = payload.popularity?.let { value ->
            popularitySnapshotsRepository.create(
                PopularitySnapshotPayload(payload.keywordId, value, payload.capturedAt)
            )
        }
        val ranks = payload.ranks.map { reading ->
            rankSnapshotsRepository.create(
                RankSnapshotPayload(
                    payload.keywordId,
                    reading.appId,
                    reading.rank,
                    reading.totalResults,
                    payload.capturedAt,
                )
            )
        }
        val topApps = payload.topApps.map { reading ->
            topAppSnapshotsRepository.create(
                TopAppSnapshotPayload(
                    payload.keywordId,
                    reading.position,
                    reading.storeAppId,
                    reading.appName,
                    payload.capturedAt,
                )
            )
        }
        return KeywordRunResult(popularity, ranks, topApps)
    }

}
