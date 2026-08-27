package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.keywords.KeywordOpportunity
import me.nathanfallet.asonar.domain.repositories.AppsRepository
import me.nathanfallet.asonar.domain.repositories.KeywordsRepository

class GetKeywordOpportunitiesUseCaseImpl(
    private val appsRepository: AppsRepository,
    private val keywordsRepository: KeywordsRepository,
    private val scoreKeywordOpportunityUseCase: ScoreKeywordOpportunityUseCase,
) : GetKeywordOpportunitiesUseCase {

    override suspend fun invoke(appId: Long): List<KeywordOpportunity>? {
        val app = appsRepository.get(appId) ?: return null
        return keywordsRepository.list(Pagination(limit = 0))
            .filter { it.store == app.store }
            .mapNotNull { scoreKeywordOpportunityUseCase(it, app) }
            // Best opportunity first; keywords we couldn't score (no score) sink to the bottom.
            .sortedByDescending { it.score ?: -1 }
    }

}
