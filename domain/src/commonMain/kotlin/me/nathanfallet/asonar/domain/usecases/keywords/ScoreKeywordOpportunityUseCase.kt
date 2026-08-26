package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.keywords.KeywordOpportunity

/** Scores one tracked keyword as an ASO opportunity for one of our apps. */
interface ScoreKeywordOpportunityUseCase {

    /** @return the opportunity read, or null if the keyword or app is unknown. */
    suspend operator fun invoke(keywordId: Long, appId: Long): KeywordOpportunity?

}
