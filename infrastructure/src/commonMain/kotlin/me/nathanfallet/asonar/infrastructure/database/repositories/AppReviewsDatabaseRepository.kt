package me.nathanfallet.asonar.infrastructure.database.repositories

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.reviews.AppReview
import me.nathanfallet.asonar.domain.models.reviews.AppReviewPayload
import me.nathanfallet.asonar.domain.repositories.AppReviewsRepository
import me.nathanfallet.asonar.infrastructure.database.TransactionManager
import me.nathanfallet.asonar.infrastructure.database.tables.AppReviews
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

class AppReviewsDatabaseRepository(
    private val transactionManager: TransactionManager,
) : AppReviewsRepository {

    init {
        transactionManager.transaction {
            SchemaUtils.create(AppReviews)
        }
    }

    override suspend fun list(
        store: Store,
        storeAppId: String,
        country: String,
        pagination: Pagination,
    ): List<AppReview> =
        transactionManager.suspendTransaction {
            AppReviews.selectAll()
                .where {
                    (AppReviews.store eq store) and
                            (AppReviews.storeAppId eq storeAppId) and
                            (AppReviews.country eq country)
                }
                .orderBy(AppReviews.postedAt to SortOrder.DESC)
                // Same convention as the other listings: limit <= 0 means "no limit".
                .apply { if (pagination.limit > 0) limit(pagination.limit.toInt()).offset(pagination.offset) }
                .map { AppReviews.toReview(it) }
        }

    override suspend fun knownExternalIds(
        store: Store,
        storeAppId: String,
        country: String,
        externalIds: List<String>,
    ): Set<String> =
        if (externalIds.isEmpty()) emptySet()
        else transactionManager.suspendTransaction {
            // Only the id column: this runs once per fetched page and never needs the review bodies.
            AppReviews.select(AppReviews.externalId)
                .where {
                    (AppReviews.store eq store) and
                            (AppReviews.storeAppId eq storeAppId) and
                            (AppReviews.country eq country) and
                            (AppReviews.externalId inList externalIds)
                }
                .map { it[AppReviews.externalId] }
                .toSet()
        }

    override suspend fun createAll(payloads: List<AppReviewPayload>): List<AppReview> =
        if (payloads.isEmpty()) emptyList()
        else transactionManager.suspendTransaction {
            AppReviews.batchInsert(payloads) { p ->
                this[AppReviews.store] = p.store
                this[AppReviews.storeAppId] = p.storeAppId
                this[AppReviews.country] = p.country
                this[AppReviews.externalId] = p.externalId
                this[AppReviews.author] = p.author
                this[AppReviews.title] = p.title
                this[AppReviews.content] = p.content
                this[AppReviews.rating] = p.rating
                this[AppReviews.version] = p.version
                this[AppReviews.postedAt] = p.postedAt
                this[AppReviews.fetchedAt] = p.fetchedAt
            }.map { AppReviews.toReview(it) }
        }

}
