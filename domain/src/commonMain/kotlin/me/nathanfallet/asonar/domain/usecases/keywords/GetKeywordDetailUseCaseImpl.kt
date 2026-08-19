package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.keywords.KeywordAppRank
import me.nathanfallet.asonar.domain.models.keywords.KeywordDetail
import me.nathanfallet.asonar.domain.repositories.AppsRepository
import me.nathanfallet.asonar.domain.repositories.KeywordsRepository
import me.nathanfallet.asonar.domain.repositories.PopularitySnapshotsRepository
import me.nathanfallet.asonar.domain.repositories.RankSnapshotsRepository
import me.nathanfallet.asonar.domain.repositories.TopAppSnapshotsRepository

class GetKeywordDetailUseCaseImpl(
    private val keywordsRepository: KeywordsRepository,
    private val popularitySnapshotsRepository: PopularitySnapshotsRepository,
    private val topAppSnapshotsRepository: TopAppSnapshotsRepository,
    private val rankSnapshotsRepository: RankSnapshotsRepository,
    private val appsRepository: AppsRepository,
) : GetKeywordDetailUseCase {

    override suspend fun invoke(keywordId: Long): KeywordDetail? {
        val keyword = keywordsRepository.get(keywordId) ?: return null
        val ranks = appsRepository.list().mapNotNull { app ->
            rankSnapshotsRepository.getLatestForKeywordAndApp(keywordId, app.id)
                ?.let { KeywordAppRank(app, it) }
        }
        return KeywordDetail(
            keyword = keyword,
            latestPopularity = popularitySnapshotsRepository.getLatestForKeyword(keywordId),
            topApps = topAppSnapshotsRepository.listLatestForKeyword(keywordId),
            ranks = ranks,
        )
    }

}
