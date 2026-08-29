package me.nathanfallet.asonar.infrastructure.database.tables

import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.reviews.AppReview
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * User reviews, keyed by the app's store identity like [AppRatingSnapshots] — so a competitor's
 * reviews are stored exactly like ours. Not a time series: a review is recorded once and never
 * rewritten.
 */
object AppReviews : LongIdTable() {

    val store = enumerationByName("store", 20, Store::class)
    val storeAppId = varchar("store_app_id", 255)
    val country = varchar("country", 2)
    val externalId = varchar("external_id", 255)
    val author = text("author").nullable()
    val title = text("title").nullable()
    val content = text("content")
    val rating = integer("rating").nullable()
    val version = varchar("version", 64).nullable()
    val postedAt = timestamp("posted_at")
    val fetchedAt = timestamp("fetched_at")

    init {
        // The dedup key. Unique so a concurrent or retried fetch can never double-record a review,
        // and it is also the index the incremental walk probes on every page.
        uniqueIndex(store, storeAppId, country, externalId)
        // Reads are "this app's reviews in this market", newest first.
        index(false, store, storeAppId, country, postedAt)
    }

    fun toReview(row: ResultRow) = AppReview(
        row[id].value,
        row[store],
        row[storeAppId],
        row[country],
        row[externalId],
        row[author],
        row[title],
        row[content],
        row[rating],
        row[version],
        row[postedAt],
        row[fetchedAt],
    )

}
