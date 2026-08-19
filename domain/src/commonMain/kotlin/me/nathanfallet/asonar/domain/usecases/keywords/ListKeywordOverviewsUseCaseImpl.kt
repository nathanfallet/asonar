package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.keywords.KeywordOverview
import me.nathanfallet.asonar.domain.repositories.KeywordsRepository
import me.nathanfallet.asonar.domain.repositories.PopularitySnapshotsRepository

class ListKeywordOverviewsUseCaseImpl(
    private val keywordsRepository: KeywordsRepository,
    private val popularitySnapshotsRepository: PopularitySnapshotsRepository,
) : ListKeywordOverviewsUseCase {

    override suspend fun invoke(pagination: Pagination): List<KeywordOverview> =
        keywordsRepository.list(pagination).map { keyword ->
            KeywordOverview(
                keyword = keyword,
                latestPopularity = popularitySnapshotsRepository.getLatestForKeyword(keyword.id),
            )
        }

}
