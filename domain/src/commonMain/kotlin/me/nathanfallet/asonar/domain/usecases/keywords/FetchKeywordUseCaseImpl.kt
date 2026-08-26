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
import kotlin.math.roundToInt
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

        // Age-gating — the snapshots already carry capturedAt, so this needs no new state. Use the two
        // dates separately: the newest of the two = "last fetch"; popularity's own date drives its gate.
        val lastPopularityAt = popularitySnapshotsRepository.getLatestForKeyword(keywordId)?.capturedAt
        val lastRankingAt = topAppSnapshotsRepository.listLatestForKeyword(keywordId).firstOrNull()?.capturedAt

        // Skip entirely if anything was fetched for this keyword within the last hour.
        val lastFetchAt = listOfNotNull(lastPopularityAt, lastRankingAt).maxOrNull()
        if (lastFetchAt != null && now - lastFetchAt < MIN_REFRESH_INTERVAL) return

        val capturedAt = now

        // Popularity is the expensive part (ASA via a real browser) and moves slowly — refetch it at
        // most weekly, keyed on ITS OWN date so daily ranking refreshes never starve it. When skipped,
        // popularity stays null → RecordKeywordRun keeps the previous snapshot as the latest value.
        val refetchPopularity = lastPopularityAt == null || now - lastPopularityAt >= POPULARITY_MAX_AGE
        val popularity = if (refetchPopularity) {
            popularitySources.firstOrNull { it.store == keyword.store }
                ?.getPopularity(keyword.term, keyword.country)
        } else {
            null
        }

        // Ranked results (App Store = iTunes; other stores later).
        val search = appSearchSources.firstOrNull { it.store == keyword.store }
            ?.search(keyword.term, keyword.country, SEARCH_LIMIT)
        val results = search?.apps.orEmpty()

        // Subtitle source for this store (App Store today; others as they're added).
        val subtitleSource = appSubtitleSources.firstOrNull { it.store == keyword.store }

        val topApps = results.take(TOP_N).mapIndexed { index, app ->
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
        val appRatings = results.map { app ->
            AppRatingReading(
                storeAppId = app.storeAppId,
                name = app.name,
                ratingCount = app.ratingCount,
                averageRating = app.averageRating,
            )
        }

        // Where each of our apps on this store lands in the results.
        val ranks = appsRepository.list()
            .filter { it.store == keyword.store }
            .map { app ->
                val index = results.indexOfFirst { it.storeAppId == app.storeAppId }
                AppRankReading(
                    appId = app.id,
                    rank = if (index >= 0) index + 1 else null,
                    totalResults = search?.totalResults,
                )
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

        // Precompute the opportunity signals (Option B): the per-competitor title-usage + review
        // velocity, done here in the background so scoring stays cheap at read time. Runs after the
        // record so each velocity regression includes this run's freshly-stored ratings.
        val competitors = topApps.map { app ->
            CompetitorSignal(
                position = app.position,
                titleFactor = OpportunityScorer.titleFactor(keyword.term, app.appName, app.subtitle),
                ratingCount = app.ratingCount,
                ratingsPer30d = getAppRatingHistoryUseCase(keyword.store, app.storeAppId, keyword.country)
                    .ratingsPerDay?.let { (it * 30).roundToInt() },
            )
        }
        keywordSignalsRepository.create(
            KeywordSignalsPayload(
                keywordId = keywordId,
                competitors = competitors,
                totalResults = search?.totalResults,
                capturedAt = capturedAt,
            )
        )
    }

    companion object {
        /** How deep we scan the results to find our apps' ranks (the top slice is used for topApps). */
        private const val SEARCH_LIMIT = 200
        private const val TOP_N = 10

        /** Don't refetch a keyword more often than this (adjustable). */
        private val MIN_REFRESH_INTERVAL = 1.hours

        /** Refetch the (expensive) popularity at most this often, on its own snapshot date (adjustable). */
        private val POPULARITY_MAX_AGE = 7.days
    }

}
