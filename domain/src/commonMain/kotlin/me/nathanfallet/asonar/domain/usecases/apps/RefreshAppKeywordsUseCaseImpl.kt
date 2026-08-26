package me.nathanfallet.asonar.domain.usecases.apps

import me.nathanfallet.asonar.domain.models.keywords.OpportunityVerdict
import me.nathanfallet.asonar.domain.usecases.keywords.GetKeywordOpportunitiesUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.RefreshKeywordUseCase

class RefreshAppKeywordsUseCaseImpl(
    private val getKeywordOpportunitiesUseCase: GetKeywordOpportunitiesUseCase,
    private val refreshKeywordUseCase: RefreshKeywordUseCase,
) : RefreshAppKeywordsUseCase {

    override suspend fun invoke(appId: Long): Int? {
        val opportunities = getKeywordOpportunitiesUseCase(appId) ?: return null
        // Keep the ones we rank on (graph freshness) + the opportunities worth watching; skip walls and
        // parked terms. Enqueue only — the fetch's age-gate drops the ones that are still fresh.
        val toRefresh = opportunities.filter { it.ourRank != null || it.verdict in RELEVANT_VERDICTS }
        toRefresh.forEach { refreshKeywordUseCase(it.keyword.id) }
        return toRefresh.size
    }

    companion object {
        private val RELEVANT_VERDICTS = setOf(OpportunityVerdict.YES, OpportunityVerdict.YES_BUT)
    }

}
