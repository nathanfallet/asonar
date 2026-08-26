package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.keywords.KeywordOpportunity

/** The recommendations for one app: every tracked keyword on its store, scored, best opportunity first. */
interface GetKeywordOpportunitiesUseCase {

    /** @return the scored keywords sorted by opportunity, or null if the app is unknown. */
    suspend operator fun invoke(appId: Long): List<KeywordOpportunity>?

}
