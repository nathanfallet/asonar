package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.runs.AppRankReading
import me.nathanfallet.asonar.domain.models.runs.AppRatingReading
import me.nathanfallet.asonar.domain.models.runs.KeywordRunPayload
import me.nathanfallet.asonar.domain.models.runs.TopAppReading
import me.nathanfallet.asonar.domain.repositories.AppsRepository
import me.nathanfallet.asonar.domain.repositories.KeywordsRepository
import me.nathanfallet.asonar.domain.services.AppSearchSource
import me.nathanfallet.asonar.domain.services.AppSubtitleSource
import me.nathanfallet.asonar.domain.services.KeywordPopularitySource
import me.nathanfallet.asonar.domain.usecases.runs.RecordKeywordRunUseCase
import kotlin.time.Clock

class FetchKeywordUseCaseImpl(
    private val keywordsRepository: KeywordsRepository,
    private val appsRepository: AppsRepository,
    private val popularitySources: List<KeywordPopularitySource>,
    private val appSearchSources: List<AppSearchSource>,
    private val appSubtitleSources: List<AppSubtitleSource>,
    private val recordKeywordRunUseCase: RecordKeywordRunUseCase,
) : FetchKeywordUseCase {

    override suspend fun invoke(keywordId: Long) {
        val keyword = keywordsRepository.get(keywordId) ?: return
        val capturedAt = Clock.System.now()

        // Popularity (App Store = ASA; other stores = their own source, when added).
        val popularity = popularitySources.firstOrNull { it.store == keyword.store }
            ?.getPopularity(keyword.term, keyword.country)

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
    }

    companion object {
        /** How deep we scan the results to find our apps' ranks (the top slice is used for topApps). */
        private const val SEARCH_LIMIT = 200
        private const val TOP_N = 10
    }

}
