package me.nathanfallet.asonar.domain.usecases.reviews

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.apps.App
import me.nathanfallet.asonar.domain.models.reviews.AppReview
import me.nathanfallet.asonar.domain.models.reviews.AppReviewPayload
import me.nathanfallet.asonar.domain.models.reviews.FetchedReview
import me.nathanfallet.asonar.domain.repositories.AppReviewsRepository
import me.nathanfallet.asonar.domain.repositories.AppsRepository
import me.nathanfallet.asonar.domain.repositories.KeywordsRepository
import me.nathanfallet.asonar.domain.services.AppReviewSource
import kotlin.time.Clock

class FetchAppReviewsUseCaseImpl(
    private val appsRepository: AppsRepository,
    private val keywordsRepository: KeywordsRepository,
    private val appReviewsRepository: AppReviewsRepository,
    private val appReviewSources: List<AppReviewSource>,
) : FetchAppReviewsUseCase {

    override suspend fun invoke(appId: Long, countries: List<String>?): List<AppReview>? {
        val app = appsRepository.get(appId) ?: return null
        val source = appReviewSources.firstOrNull { it.store == app.store } ?: return emptyList()

        val markets = countries?.map { it.trim().uppercase() }?.filter { it.isNotEmpty() }?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?: trackedMarkets(app)

        val fetchedAt = Clock.System.now()
        // Collect every market first, write once. Filtering and mapping happen here so the repository
        // only ever sees rows that are new — one batch insert for the whole run instead of a statement
        // per market (let alone per review).
        val payloads = markets.flatMap { country ->
            collectNew(source, app, country).map { it.toPayload(app, country, fetchedAt) }
        }
        return appReviewsRepository.createAll(payloads)
    }

    /**
     * The markets worth fetching: those where at least one keyword is tracked on the app's store.
     * Reviews from a market we track nothing in would never be read by anything downstream.
     */
    private suspend fun trackedMarkets(app: App): List<String> =
        keywordsRepository.list(Pagination(limit = 0))
            .filter { it.store == app.store }
            .map { it.country }
            .distinct()

    /**
     * Walks a market newest-first and returns only what we don't already have, **stopping at the
     * first review we know**: the source orders by recency, so anything behind a known review is
     * older and therefore already recorded. After the first run this reads a single page per market.
     *
     * Within a page we still keep only the unknown reviews rather than assuming the page is uniformly
     * new — a page boundary can straddle the frontier, and the same review can reappear a page later
     * when new ones shift the window.
     */
    private suspend fun collectNew(
        source: AppReviewSource,
        app: App,
        country: String,
    ): List<FetchedReview> {
        val collected = mutableListOf<FetchedReview>()
        val seen = mutableSetOf<String>()
        for (page in 1..source.maxPages) {
            val reviews = source.getReviews(app.storeAppId, country, page)
            // An empty page is a normal answer (plenty of apps expose no reviews in a given market),
            // and it also marks the end of the feed — either way there is nothing further to walk.
            if (reviews.isEmpty()) break

            val known = appReviewsRepository.knownExternalIds(
                app.store,
                app.storeAppId,
                country,
                reviews.map { it.externalId },
            )
            val fresh = reviews.takeWhile { it.externalId !in known }
            fresh.forEach { review -> if (seen.add(review.externalId)) collected += review }
            // Hit the frontier: this page contained something we already had.
            if (fresh.size < reviews.size) break
        }
        return collected
    }

    private fun FetchedReview.toPayload(app: App, country: String, fetchedAt: kotlin.time.Instant) =
        AppReviewPayload(
            store = app.store,
            storeAppId = app.storeAppId,
            country = country,
            externalId = externalId,
            author = author,
            title = title,
            content = content,
            rating = rating,
            version = version,
            postedAt = postedAt,
            fetchedAt = fetchedAt,
        )

}
