package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshot
import me.nathanfallet.asonar.domain.repositories.PopularitySnapshotsRepository

class ListPopularityHistoryUseCaseImpl(
    private val popularitySnapshotsRepository: PopularitySnapshotsRepository,
) : ListPopularityHistoryUseCase {

    override suspend fun invoke(keywordId: Long, pagination: Pagination): List<PopularitySnapshot> =
        popularitySnapshotsRepository.listForKeyword(keywordId, pagination)

}
