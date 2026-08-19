package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.snapshots.TopAppSnapshot
import me.nathanfallet.asonar.domain.repositories.TopAppSnapshotsRepository

class GetLatestTopAppsUseCaseImpl(
    private val topAppSnapshotsRepository: TopAppSnapshotsRepository,
) : GetLatestTopAppsUseCase {

    override suspend fun invoke(keywordId: Long): List<TopAppSnapshot> =
        topAppSnapshotsRepository.listLatestForKeyword(keywordId)

}
