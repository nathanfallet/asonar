package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.snapshots.RankSnapshot
import me.nathanfallet.asonar.domain.repositories.RankSnapshotsRepository

class ListRankHistoryUseCaseImpl(
    private val rankSnapshotsRepository: RankSnapshotsRepository,
) : ListRankHistoryUseCase {

    override suspend fun invoke(
        keywordId: Long,
        appId: Long,
        pagination: Pagination,
    ): List<RankSnapshot> =
        rankSnapshotsRepository.listForKeywordAndApp(keywordId, appId, pagination)

}
