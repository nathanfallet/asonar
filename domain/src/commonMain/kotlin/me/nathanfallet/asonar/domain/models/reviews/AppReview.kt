package me.nathanfallet.asonar.domain.models.reviews

import kotlinx.serialization.Serializable
import me.nathanfallet.asonar.domain.models.apps.Store
import kotlin.time.Instant

/**
 * One user review of an app in one market. Like the rating snapshots, reviews hang off the app's
 * **store identity** (store + storeAppId + country) rather than one of our [App][me.nathanfallet.asonar.domain.models.apps.App]
 * rows: that way a competitor's reviews are stored exactly like our own, and stay readable even if
 * the app is later untracked.
 *
 * Unlike the snapshot tables this is not a time series — a review is a fact that happened once.
 * [externalId] is the store's own id for it and the dedup key: re-fetching a market must never
 * duplicate what we already have.
 */
@Serializable
data class AppReview(
    val id: Long,
    val store: Store,
    val storeAppId: String,
    val country: String, // ISO 3166-1 alpha-2, e.g. "FR"
    val externalId: String, // the store's review id — unique per (store, app, country)
    val author: String? = null,
    val title: String? = null,
    val content: String,
    val rating: Int? = null, // 1..5
    val version: String? = null, // the app version reviewed, when the store says
    val postedAt: Instant, // when the review was written, per the store
    val fetchedAt: Instant, // when we read it
)

/** What it takes to record an [AppReview]. */
@Serializable
data class AppReviewPayload(
    val store: Store,
    val storeAppId: String,
    val country: String,
    val externalId: String,
    val author: String? = null,
    val title: String? = null,
    val content: String,
    val rating: Int? = null,
    val version: String? = null,
    val postedAt: Instant,
    val fetchedAt: Instant,
)
