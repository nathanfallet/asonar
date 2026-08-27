package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.keywords.KeywordOpportunity
import me.nathanfallet.asonar.domain.repositories.AppsRepository
import me.nathanfallet.asonar.domain.repositories.KeywordSignalsRepository
import me.nathanfallet.asonar.domain.repositories.KeywordsRepository
import me.nathanfallet.asonar.domain.repositories.PopularitySnapshotsRepository
import me.nathanfallet.asonar.domain.repositories.RankSnapshotsRepository
import me.nathanfallet.asonar.domain.usecases.apps.GetAppRatingHistoryUseCase

class GetKeywordOpportunitiesUseCaseImpl(
    private val appsRepository: AppsRepository,
    private val keywordsRepository: KeywordsRepository,
    private val keywordSignalsRepository: KeywordSignalsRepository,
    private val popularitySnapshotsRepository: PopularitySnapshotsRepository,
    private val rankSnapshotsRepository: RankSnapshotsRepository,
    private val getAppRatingHistoryUseCase: GetAppRatingHistoryUseCase,
    private val scoreKeywordOpportunityUseCase: ScoreKeywordOpportunityUseCase,
) : GetKeywordOpportunitiesUseCase {

    override suspend fun invoke(appId: Long): List<KeywordOpportunity>? {
        val app = appsRepository.get(appId) ?: return null
        val keywords = keywordsRepository.list(Pagination(limit = 0)).filter { it.store == app.store }

        // Batch-load everything the scorer needs, once, instead of a handful of queries per keyword (the
        // N+1 — this was the slowest page). Our velocity only depends on the market (our app × country),
        // so it's computed once per distinct country, not per keyword.
        val signalsByKeyword = keywordSignalsRepository.latestByKeyword()
        val popularityByKeyword = popularitySnapshotsRepository.latestByKeyword()
        val rankByKeyword = rankSnapshotsRepository.latestByKeywordForApp(app.id)
        val velocityByCountry = keywords.map { it.country }.distinct().associateWith { country ->
            getAppRatingHistoryUseCase(app.store, app.storeAppId, country).ratingsPer30d
        }

        return keywords.mapNotNull { keyword ->
            scoreKeywordOpportunityUseCase(
                keyword = keyword,
                signals = signalsByKeyword[keyword.id],
                popularity = popularityByKeyword[keyword.id]?.popularity,
                ourRank = rankByKeyword[keyword.id],
                ourVelocity = velocityByCountry[keyword.country],
            )
        }
            // Best opportunity first; keywords we couldn't score (no score) sink to the bottom.
            .sortedByDescending { it.score ?: -1 }
    }

}
