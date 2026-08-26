package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.keywords.KeywordOpportunity
import me.nathanfallet.asonar.domain.repositories.*
import me.nathanfallet.asonar.domain.usecases.apps.GetAppRatingHistoryUseCase
import kotlin.math.roundToInt

/**
 * Scores a keyword at read time from the **precomputed per-competitor signals** (Option B): the
 * expensive top-of-results title-usage + review velocity are read from [KeywordSignalsRepository]
 * (captured at fetch), so this only adds the cheap, app-specific bits — our rank and our own review
 * velocity — before running the pure [OpportunityScorer]. Weights/thresholds re-tune without a re-fetch.
 */
class ScoreKeywordOpportunityUseCaseImpl(
    private val keywordsRepository: KeywordsRepository,
    private val appsRepository: AppsRepository,
    private val popularitySnapshotsRepository: PopularitySnapshotsRepository,
    private val keywordSignalsRepository: KeywordSignalsRepository,
    private val rankSnapshotsRepository: RankSnapshotsRepository,
    private val getAppRatingHistoryUseCase: GetAppRatingHistoryUseCase,
) : ScoreKeywordOpportunityUseCase {

    override suspend fun invoke(keywordId: Long, appId: Long): KeywordOpportunity? {
        val keyword = keywordsRepository.get(keywordId) ?: return null
        val app = appsRepository.get(appId) ?: return null

        val signals = keywordSignalsRepository.getLatestForKeyword(keywordId)
        val competitors = signals?.competitors.orEmpty()
        val popularity = popularitySnapshotsRepository.getLatestForKeyword(keywordId)?.popularity
        val ourRankSnapshot = rankSnapshotsRepository.getLatestForKeywordAndApp(keywordId, appId)
        val ourVelocity = getAppRatingHistoryUseCase(keyword.store, app.storeAppId, keyword.country)
            .ratingsPer30d

        val totalResults = signals?.totalResults ?: ourRankSnapshot?.totalResults
        val result = OpportunityScorer.score(
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
            ourRank = ourRankSnapshot?.rank,
            ourRatingsPer30d = ourVelocity,
            top10MedianRatingsPer30d = result.top10MedianVelocity,
            velocityAdvantage = result.velocityAdvantage,
            wallStrength = result.wallStrength,
            top10TitleUsage = OpportunityScorer.titleShare(competitors),
            totalResults = totalResults,
            comment = comment(
                competitors,
                result.wallStrength,
                result.top10MedianVelocity,
                ourVelocity,
                result.velocityAdvantage,
                ourRankSnapshot?.rank,
                totalResults
            ),
        )
    }

    private fun comment(
        competitors: List<me.nathanfallet.asonar.domain.models.keywords.CompetitorSignal>,
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
