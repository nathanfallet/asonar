package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.apps.App
import me.nathanfallet.asonar.domain.models.keywords.Keyword
import me.nathanfallet.asonar.domain.models.keywords.KeywordOpportunity

/** Scores one tracked keyword as an ASO opportunity for one of our apps. */
interface ScoreKeywordOpportunityUseCase {

    /**
     * Scores [keyword] as an opportunity for [app]. Both are passed in (the caller has already listed
     * them) to avoid a per-keyword re-fetch. @return the opportunity, or null if no scorer for the store.
     */
    suspend operator fun invoke(keyword: Keyword, app: App): KeywordOpportunity?

}
