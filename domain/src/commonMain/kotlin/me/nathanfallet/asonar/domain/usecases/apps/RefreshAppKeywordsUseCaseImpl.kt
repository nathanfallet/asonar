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
        // Enqueue only — the fetch's age-gate drops the ones that are still fresh.
        val toRefresh = opportunities.filter { shouldRefresh(it.ourRank != null, it.verdict) }
        toRefresh.forEach { refreshKeywordUseCase(it.keyword.id) }
        return toRefresh.size
    }

    companion object {
        /**
         * Refresh the keywords we rank on (keep the graph fresh) plus the ones still worth watching:
         * opportunities to chase (YES / YES_BUT) and **pending** ones (UNKNOWN = not enough data yet,
         * so re-fetch to let them resolve). Skip settled walls (NO) and parked low-volume terms
         * (RESERVE) — re-scoring them from scratch won't change the call.
         */
        internal fun shouldRefresh(ranked: Boolean, verdict: OpportunityVerdict): Boolean =
            ranked || verdict in RELEVANT_VERDICTS

        private val RELEVANT_VERDICTS = setOf(
            OpportunityVerdict.YES,
            OpportunityVerdict.YES_BUT,
            OpportunityVerdict.UNKNOWN,
        )
    }

}
