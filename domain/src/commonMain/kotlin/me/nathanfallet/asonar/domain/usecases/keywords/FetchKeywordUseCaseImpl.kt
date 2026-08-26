package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.keywords.CompetitorSignal
import me.nathanfallet.asonar.domain.models.keywords.KeywordSignalsPayload
import me.nathanfallet.asonar.domain.models.runs.AppRankReading
import me.nathanfallet.asonar.domain.models.runs.AppRatingReading
import me.nathanfallet.asonar.domain.models.runs.KeywordRunPayload
import me.nathanfallet.asonar.domain.models.runs.TopAppReading
import me.nathanfallet.asonar.domain.repositories.*
import me.nathanfallet.asonar.domain.services.AppSearchSource
import me.nathanfallet.asonar.domain.services.AppSubtitleSource
import me.nathanfallet.asonar.domain.services.KeywordPopularitySource
import me.nathanfallet.asonar.domain.usecases.apps.GetAppRatingHistoryUseCase
import me.nathanfallet.asonar.domain.usecases.runs.RecordKeywordRunUseCase
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class FetchKeywordUseCaseImpl(
    private val keywordsRepository: KeywordsRepository,
    private val appsRepository: AppsRepository,
    private val popularitySources: List<KeywordPopularitySource>,
    private val appSearchSources: List<AppSearchSource>,
    private val appSubtitleSources: List<AppSubtitleSource>,
    private val recordKeywordRunUseCase: RecordKeywordRunUseCase,
    private val getAppRatingHistoryUseCase: GetAppRatingHistoryUseCase,
    private val keywordSignalsRepository: KeywordSignalsRepository,
    private val popularitySnapshotsRepository: PopularitySnapshotsRepository,
    private val topAppSnapshotsRepository: TopAppSnapshotsRepository,
) : FetchKeywordUseCase {

    override suspend fun invoke(keywordId: Long) {
        val keyword = keywordsRepository.get(keywordId) ?: return
        val now = Clock.System.now()

        // Independent age gates — the snapshots already carry capturedAt (no new state). Ranking and
        // popularity are refreshed on their OWN dates, independently of each other: if popularity is
        // still fresh but the ranking is stale (or failed last time) we refetch the ranking alone, and
        // vice-versa. We only do nothing when both are within their windows.
        val lastRankingAt = topAppSnapshotsRepository.listLatestForKeyword(keywordId).firstOrNull()?.capturedAt
        val lastPopularityAt = popularitySnapshotsRepository.getLatestForKeyword(keywordId)?.capturedAt
        val fetchRanking = lastRankingAt == null || now - lastRankingAt >= RANKING_MAX_AGE
        val fetchPopularity = lastPopularityAt == null || now - lastPopularityAt >= POPULARITY_MAX_AGE
        if (!fetchRanking && !fetchPopularity) return

        val capturedAt = now

        // 1) Ranking first (App Store = iTunes; other stores later): the top-of-results, our apps' ranks,
        //    and the per-competitor signals — all derived from a single search.
        var topApps = emptyList<TopAppReading>()
        var appRatings = emptyList<AppRatingReading>()
        var ranks = emptyList<AppRankReading>()
        var totalResults: Int? = null
        if (fetchRanking) {
            val search = appSearchSources.firstOrNull { it.store == keyword.store }
                ?.search(keyword.term, keyword.country, SEARCH_LIMIT)
            val results = search?.apps.orEmpty()
            totalResults = search?.totalResults
            val subtitleSource = appSubtitleSources.firstOrNull { it.store == keyword.store }
            topApps = results.take(TOP_N).mapIndexed { index, app ->
                TopAppReading(
                    position = index + 1,
                    storeAppId = app.storeAppId,
                    appName = app.name,
                    // Prefer the subtitle the search already carried (some stores, e.g. Play, include the
                    // short description in results); only fall back to the dedicated source — an extra
                    // fetch — when the search didn't provide it (the case for the iTunes API).
                    subtitle = app.subtitle ?: subtitleSource?.getSubtitle(app.storeAppId, keyword.country),
                    ratingCount = app.ratingCount,
                    averageRating = app.averageRating,
                )
            }
            // Ratings for every app seen — recorded per app/store/country (shared across keywords).
            appRatings = results.map { app ->
                AppRatingReading(
                    storeAppId = app.storeAppId,
                    name = app.name,
                    ratingCount = app.ratingCount,
                    averageRating = app.averageRating,
                )
            }
            // Where each of our apps on this store lands in the results.
            ranks = appsRepository.list()
                .filter { it.store == keyword.store }
                .map { app ->
                    val index = results.indexOfFirst { it.storeAppId == app.storeAppId }
                    AppRankReading(
                        appId = app.id,
                        rank = if (index >= 0) index + 1 else null,
                        totalResults = totalResults,
                    )
                }
        }

        // 2) Popularity second (the expensive ASA call via a real browser). When skipped it stays null →
        //    RecordKeywordRun keeps the previous popularity snapshot as the latest value.
        val popularity = if (fetchPopularity) {
            popularitySources.firstOrNull { it.store == keyword.store }
                ?.getPopularity(keyword.term, keyword.country)
        } else {
            null
        }

        recordKeywordRunUseCase(
            KeywordRunPayload(
                keywordId = keywordId,
                store = keyword.store,
                country = keyword.country,
                capturedAt = capturedAt,
                popularity = popularity,
                ranks = ranks,
                topApps = topApps,
                appRatings = appRatings,
            )
        )

        // Recompute the opportunity signals (Option B) only when we refreshed the ranking — they're
        // derived from the top-of-results. Runs after the record so each velocity regression includes
        // this run's freshly-stored ratings.
        if (fetchRanking) {
            val competitors = topApps.map { app ->
                CompetitorSignal(
                    position = app.position,
                    titleFactor = OpportunityScorer.titleFactor(keyword.term, app.appName, app.subtitle),
                    ratingCount = app.ratingCount,
                    ratingsPer30d = getAppRatingHistoryUseCase(keyword.store, app.storeAppId, keyword.country)
                        .ratingsPer30d,
                )
            }
            keywordSignalsRepository.create(
                KeywordSignalsPayload(
                    keywordId = keywordId,
                    competitors = competitors,
                    totalResults = totalResults,
                    capturedAt = capturedAt,
                )
            )
        }
    }

    companion object {
        /** How deep we scan the results to find our apps' ranks (the top slice is used for topApps). */
        private const val SEARCH_LIMIT = 200
        private const val TOP_N = 10

        /** Refetch the ranking (top-of-results + our ranks) at most this often, on its own date (adjustable). */
        private val RANKING_MAX_AGE = 1.hours

        /** Refetch the (expensive) popularity at most this often, on its own snapshot date (adjustable). */
        private val POPULARITY_MAX_AGE = 7.days
    }

}
