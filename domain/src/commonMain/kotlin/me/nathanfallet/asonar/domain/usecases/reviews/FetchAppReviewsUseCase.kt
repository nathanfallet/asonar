package me.nathanfallet.asonar.domain.usecases.reviews

import me.nathanfallet.asonar.domain.models.reviews.AppReview

/**
 * Collects an app's user reviews across the markets we care about and records the ones we don't have
 * yet. Reviews are the raw material for keyword discovery — the words users actually type about an
 * app — and this works on any registered app, ours or a competitor's.
 *
 * **Markets.** Left to itself it fetches the countries where at least one keyword is tracked on the
 * app's store. Fetching a market we track nothing in would collect reviews nobody will ever read, so
 * the tracked keywords define the perimeter.
 *
 * **Incremental.** Sources return reviews newest-first, so the walk stops at the first review already
 * in the database: everything behind it is older, therefore already known. Only the first run of a
 * market pays for a full walk.
 */
interface FetchAppReviewsUseCase {

    /**
     * @param appId The app to collect reviews for.
     * @param countries Markets to fetch, ISO alpha-2. Null/empty = every market the app's store has
     *   tracked keywords in.
     * @return The reviews newly recorded, or null if the app doesn't exist.
     */
    suspend operator fun invoke(appId: Long, countries: List<String>? = null): List<AppReview>?

}
