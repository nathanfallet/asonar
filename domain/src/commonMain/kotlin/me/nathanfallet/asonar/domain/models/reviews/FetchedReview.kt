package me.nathanfallet.asonar.domain.models.reviews

import kotlin.time.Instant

/**
 * One review as a source read it, before we decide whether to keep it. Deliberately separate from
 * [AppReview]: a source only knows what the store told it (it doesn't know the app row, nor when we
 * chose to record it), and the orchestrator is what turns this into a payload.
 *
 * [externalId] must be the store's own identifier for the review — it is what lets a later fetch
 * recognize a review it already has.
 */
data class FetchedReview(
    val externalId: String,
    val content: String,
    val postedAt: Instant,
    val author: String? = null,
    val title: String? = null,
    val rating: Int? = null,
    val version: String? = null,
)
