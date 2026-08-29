package me.nathanfallet.asonar.domain.repositories

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.reviews.AppReview
import me.nathanfallet.asonar.domain.models.reviews.AppReviewPayload

/** Reads and writes the user reviews we collected, keyed by the app's store identity. */
interface AppReviewsRepository {

    /** An app's reviews in a market, newest first. */
    suspend fun list(
        store: Store,
        storeAppId: String,
        country: String,
        pagination: Pagination,
    ): List<AppReview>

    /**
     * Which of [externalIds] we already hold for this app and market. One query for a whole page of
     * reviews, so the incremental walk costs a single read per page instead of one per review.
     */
    suspend fun knownExternalIds(
        store: Store,
        storeAppId: String,
        country: String,
        externalIds: List<String>,
    ): Set<String>

    /** Records reviews in ONE batch insert. @return The stored rows. */
    suspend fun createAll(payloads: List<AppReviewPayload>): List<AppReview>

}
