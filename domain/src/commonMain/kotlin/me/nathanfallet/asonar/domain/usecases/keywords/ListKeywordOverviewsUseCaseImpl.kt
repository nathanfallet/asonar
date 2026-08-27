package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.keywords.KeywordOverview
import me.nathanfallet.asonar.domain.repositories.KeywordsRepository
import me.nathanfallet.asonar.domain.repositories.PopularitySnapshotsRepository

class ListKeywordOverviewsUseCaseImpl(
    private val keywordsRepository: KeywordsRepository,
    private val popularitySnapshotsRepository: PopularitySnapshotsRepository,
) : ListKeywordOverviewsUseCase {

    override suspend fun invoke(pagination: Pagination): List<KeywordOverview> {
        // One read for every keyword's latest popularity, instead of a getLatest per keyword (the N+1).
        val popularityByKeyword = popularitySnapshotsRepository.latestByKeyword()
        return keywordsRepository.list(pagination).map { keyword ->
            KeywordOverview(
                keyword = keyword,
                latestPopularity = popularityByKeyword[keyword.id],
            )
        }
    }

}
