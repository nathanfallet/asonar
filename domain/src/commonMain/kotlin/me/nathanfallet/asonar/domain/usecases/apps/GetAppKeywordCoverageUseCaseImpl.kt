package me.nathanfallet.asonar.domain.usecases.apps

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.apps.AppKeywordCoverage
import me.nathanfallet.asonar.domain.models.apps.KeywordCoverageEntry
import me.nathanfallet.asonar.domain.models.apps.RankPoint
import me.nathanfallet.asonar.domain.repositories.AppsRepository
import me.nathanfallet.asonar.domain.repositories.KeywordsRepository
import me.nathanfallet.asonar.domain.repositories.PopularitySnapshotsRepository
import me.nathanfallet.asonar.domain.repositories.RankSnapshotsRepository

class GetAppKeywordCoverageUseCaseImpl(
    private val appsRepository: AppsRepository,
    private val keywordsRepository: KeywordsRepository,
    private val rankSnapshotsRepository: RankSnapshotsRepository,
    private val popularitySnapshotsRepository: PopularitySnapshotsRepository,
) : GetAppKeywordCoverageUseCase {

    override suspend fun invoke(appId: Long): AppKeywordCoverage? {
        val app = appsRepository.get(appId) ?: return null
        val keywords = keywordsRepository.list(Pagination(limit = 1000))
            .filter { it.store == app.store }
        val entries = keywords.map { keyword ->
            // newest-first history; latest reading is the current standing, reversed for the graph.
            val history = rankSnapshotsRepository.listForKeywordAndApp(keyword.id, appId, Pagination(limit = 500))
            val latest = history.firstOrNull()
            KeywordCoverageEntry(
                keyword = keyword,
                popularity = popularitySnapshotsRepository.getLatestForKeyword(keyword.id)?.popularity,
                rank = latest?.rank,
                totalResults = latest?.totalResults,
                capturedAt = latest?.capturedAt,
                history = history.reversed().map { RankPoint(it.rank, it.capturedAt) },
            )
        }.sortedWith(
            // Ranked first (best rank on top), then the gaps to chase, popularity breaking ties.
            compareBy({ it.rank == null }, { it.rank ?: Int.MAX_VALUE }, { -(it.popularity ?: 0) }),
        )
        return AppKeywordCoverage(app, entries)
    }

}
