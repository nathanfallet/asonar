package me.nathanfallet.asonar.domain.services

import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.reviews.FetchedReview

/**
 * A source of user reviews for one [store]. Reviews are raw material for keyword discovery (the words
 * users actually type about an app) and work on any app, ours or a competitor's.
 *
 * Paged and **newest-first**, because that is what makes incremental fetching possible: the caller
 * walks pages until it meets a review it already has, and stops — everything behind it is older, so
 * already known. A source that cannot honour that ordering breaks the contract.
 *
 * Returning an empty list is a **normal answer**, not a failure: a real chunk of apps simply expose
 * no reviews in a given market (measured on the App Store feed). A source that actually failed logs
 * it and still returns empty, so one bad market can't sink a whole run.
 */
interface AppReviewSource {

    /** Which store this source covers. */
    val store: Store

    /** How deep the store lets us page. The caller never asks beyond this. */
    val maxPages: Int

    /** Reviews for [storeAppId] in [country], newest first. [page] is 1-based. */
    suspend fun getReviews(storeAppId: String, country: String, page: Int): List<FetchedReview>

}
