package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.keywords.Keyword
import me.nathanfallet.asonar.domain.models.keywords.KeywordOpportunity
import me.nathanfallet.asonar.domain.models.keywords.KeywordSignals
import me.nathanfallet.asonar.domain.models.snapshots.RankSnapshot

/** Scores one tracked keyword as an ASO opportunity for one of our apps. */
interface ScoreKeywordOpportunityUseCase {

    /**
     * Scores [keyword] from data the caller has already loaded — the precomputed [signals], the latest
     * [popularity], our latest [ourRank] and our review velocity [ourVelocity] — so it does no I/O and
     * the caller can batch-load once and score every keyword in memory. @return the opportunity, or
     * null if no scorer is registered for the keyword's store.
     */
    suspend operator fun invoke(
        keyword: Keyword,
        signals: KeywordSignals?,
        popularity: Int?,
        ourRank: RankSnapshot?,
        ourVelocity: Int?,
    ): KeywordOpportunity?

}
