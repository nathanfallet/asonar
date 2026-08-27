package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.keywords.CompetitorSignal
import me.nathanfallet.asonar.domain.models.keywords.Keyword
import me.nathanfallet.asonar.domain.models.keywords.KeywordOpportunity
import me.nathanfallet.asonar.domain.models.keywords.KeywordSignals
import me.nathanfallet.asonar.domain.models.snapshots.RankSnapshot
import kotlin.math.roundToInt

/**
 * Scores a keyword as an ASO opportunity from data the caller has **already loaded** (Option B): the
 * precomputed per-competitor [signals] (title-usage + review velocity, captured at fetch), the latest
 * [popularity], our latest [ourRank] and our own review velocity [ourVelocity]. Pure — no I/O — so the
 * caller batch-loads once and scores every keyword in memory instead of a query per keyword. Runs the
 * store's [OpportunityScorer]; weights/thresholds re-tune without a re-fetch.
 */
class ScoreKeywordOpportunityUseCaseImpl(
    private val opportunityScorers: List<OpportunityScorer>,
) : ScoreKeywordOpportunityUseCase {

    override suspend fun invoke(
        keyword: Keyword,
        signals: KeywordSignals?,
        popularity: Int?,
        ourRank: RankSnapshot?,
        ourVelocity: Int?,
    ): KeywordOpportunity? {
        val scorer = opportunityScorers.firstOrNull { it.store == keyword.store } ?: return null

        val competitors = signals?.competitors.orEmpty()
        val totalResults = signals?.totalResults ?: ourRank?.totalResults
        val result = scorer.score(
            OpportunityScorer.Inputs(
                popularity = popularity,
                competitors = competitors,
                ourVelocity = ourVelocity,
                totalResults = totalResults,
            )
        )

        return KeywordOpportunity(
            keyword = keyword,
            verdict = result.verdict,
            score = result.score,
            popularity = popularity,
            ourRank = ourRank?.rank,
            ourRatingsPer30d = ourVelocity,
            top10MedianRatingsPer30d = result.top10MedianVelocity,
            velocityAdvantage = result.velocityAdvantage,
            wallStrength = result.wallStrength,
            top10TitleUsage = scorer.titleShare(competitors),
            totalResults = totalResults,
            comment = comment(
                competitors,
                result.wallStrength,
                result.top10MedianVelocity,
                ourVelocity,
                result.velocityAdvantage,
                ourRank?.rank,
                totalResults
            ),
        )
    }

    private fun comment(
        competitors: List<CompetitorSignal>,
        wallStrength: Double,
        top10Velocity: Int?,
        ourVelocity: Int?,
        velAdvantage: Double?,
        ourRank: Int?,
        totalResults: Int?,
    ): String {
        if (competitors.isEmpty()) return "Pas encore de données (rafraîchis le mot-clé)."
        val wallPct = (wallStrength * 100).roundToInt()
        val leader = competitors.minByOrNull { it.position }
        val leaderNote = leader?.let {
            val uses =
                if (it.titleFactor >= 1.0) "utilise le terme" else if (it.titleFactor > 0) "terme en sous-titre" else "n'utilise pas le terme"
            val reviews =
                it.ratingsPer30d?.let { v -> "~$v avis/30j" } ?: it.ratingCount?.let { n -> "$n notes" } ?: "notes n/c"
            "#1 $uses, $reviews"
        } ?: ""
        val theirs = top10Velocity?.let { "~$it avis/30j" } ?: "n/a"
        val ours = ourVelocity?.let { "~$it avis/30j" } ?: "n/a"
        val adv = velAdvantage?.let { " (avantage ${((it * 10).roundToInt() / 10.0)}×)" } ?: ""
        val rank = ourRank?.let { "#$it" } ?: "non ranké"
        val results = totalResults?.let { "$it résultats" } ?: "résultats n/c"
        return "Force du mur $wallPct% · $leaderNote · vélocité leaders $theirs vs toi $ours$adv · ton rang $rank · $results"
    }

}
