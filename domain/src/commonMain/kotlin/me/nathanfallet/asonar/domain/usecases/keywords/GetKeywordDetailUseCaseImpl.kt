package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.keywords.KeywordAppRank
import me.nathanfallet.asonar.domain.models.keywords.KeywordDetail
import me.nathanfallet.asonar.domain.models.keywords.KeywordTopApp
import me.nathanfallet.asonar.domain.repositories.*
import me.nathanfallet.asonar.domain.usecases.apps.GetAppRatingHistoryUseCase

class GetKeywordDetailUseCaseImpl(
    private val keywordsRepository: KeywordsRepository,
    private val popularitySnapshotsRepository: PopularitySnapshotsRepository,
    private val topAppSnapshotsRepository: TopAppSnapshotsRepository,
    private val rankSnapshotsRepository: RankSnapshotsRepository,
    private val appsRepository: AppsRepository,
    private val getAppRatingHistoryUseCase: GetAppRatingHistoryUseCase,
) : GetKeywordDetailUseCase {

    override suspend fun invoke(keywordId: Long): KeywordDetail? {
        val keyword = keywordsRepository.get(keywordId) ?: return null
        val ranks = appsRepository.list().mapNotNull { app ->
            rankSnapshotsRepository.getLatestForKeywordAndApp(keywordId, app.id)
                ?.let { KeywordAppRank(app, it) }
        }
        // Enrich each top-of-results app with its rating velocity. The history is keyed by the app +
        // this keyword's market (store/country), and shared across every keyword the app appears on.
        val topApps = topAppSnapshotsRepository.listLatestForKeyword(keywordId).map { snapshot ->
            val history = getAppRatingHistoryUseCase(keyword.store, snapshot.storeAppId, keyword.country)
            KeywordTopApp(snapshot = snapshot, ratingsPerDay = history.ratingsPerDay)
        }
        return KeywordDetail(
            keyword = keyword,
            latestPopularity = popularitySnapshotsRepository.getLatestForKeyword(keywordId),
            topApps = topApps,
            ranks = ranks,
        )
    }

}
