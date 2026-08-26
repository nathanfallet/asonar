package me.nathanfallet.asonar.domain.usecases.apps

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.apps.AppKeywordCoverage
import me.nathanfallet.asonar.domain.models.apps.CoverageSummary
import me.nathanfallet.asonar.domain.models.apps.KeywordCoverageEntry
import me.nathanfallet.asonar.domain.models.apps.RankPoint
import me.nathanfallet.asonar.domain.repositories.AppsRepository
import me.nathanfallet.asonar.domain.repositories.KeywordsRepository
import me.nathanfallet.asonar.domain.repositories.PopularitySnapshotsRepository
import me.nathanfallet.asonar.domain.repositories.RankSnapshotsRepository
import kotlin.math.roundToInt

class GetAppKeywordCoverageUseCaseImpl(
    private val appsRepository: AppsRepository,
    private val keywordsRepository: KeywordsRepository,
    private val rankSnapshotsRepository: RankSnapshotsRepository,
    private val popularitySnapshotsRepository: PopularitySnapshotsRepository,
) : GetAppKeywordCoverageUseCase {

    override suspend fun invoke(appId: Long): AppKeywordCoverage? {
        val app = appsRepository.get(appId) ?: return null
        val keywords = keywordsRepository.list(Pagination(limit = 0))
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
        return AppKeywordCoverage(app, summarize(entries), entries)
    }

    private fun summarize(entries: List<KeywordCoverageEntry>): CoverageSummary {
        val ranks = entries.mapNotNull { it.rank }
        // Movement vs the immediately previous reading (needs a comparable prior rank on that keyword).
        var up = 0
        var down = 0
        var flat = 0
        for (entry in entries) {
            val current = entry.rank ?: continue
            val previous = entry.history.getOrNull(entry.history.size - 2)?.rank ?: continue
            when {
                current < previous -> up++   // lower number = better
                current > previous -> down++
                else -> flat++
            }
        }
        return CoverageSummary(
            averageRank = ranks.takeIf { it.isNotEmpty() }?.average()?.roundToInt(),
            bestRank = ranks.minOrNull(),
            worstRank = ranks.maxOrNull(),
            rankedCount = ranks.size,
            trackedCount = entries.size,
            top5 = ranks.count { it <= 5 },
            top25 = ranks.count { it in 6..25 },
            top100 = ranks.count { it in 26..100 },
            beyond100 = ranks.count { it > 100 },
            wentUp = up,
            wentDown = down,
            unchanged = flat,
        )
    }

}
