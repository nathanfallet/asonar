package me.nathanfallet.asonar.infrastructure.database.repositories

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.snapshots.AppRatingSnapshot
import me.nathanfallet.asonar.domain.models.snapshots.AppRatingSnapshotPayload
import me.nathanfallet.asonar.domain.repositories.AppRatingSnapshotsRepository
import me.nathanfallet.asonar.infrastructure.database.TransactionManager
import me.nathanfallet.asonar.infrastructure.database.tables.AppRatingSnapshots
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll

class AppRatingSnapshotsDatabaseRepository(
    private val transactionManager: TransactionManager,
) : AppRatingSnapshotsRepository {

    init {
        transactionManager.transaction {
            SchemaUtils.create(AppRatingSnapshots)
        }
    }

    override suspend fun create(payload: AppRatingSnapshotPayload): AppRatingSnapshot =
        transactionManager.suspendTransaction {
            val newId = AppRatingSnapshots.insertAndGetId {
                it[store] = payload.store
                it[storeAppId] = payload.storeAppId
                it[country] = payload.country
                it[name] = payload.name
                it[ratingCount] = payload.ratingCount
                it[averageRating] = payload.averageRating
                it[capturedAt] = payload.capturedAt
            }.value
            AppRatingSnapshot(
                newId,
                payload.store,
                payload.storeAppId,
                payload.country,
                payload.name,
                payload.ratingCount,
                payload.averageRating,
                payload.capturedAt,
            )
        }

    override suspend fun createAll(payloads: List<AppRatingSnapshotPayload>): List<AppRatingSnapshot> =
        if (payloads.isEmpty()) emptyList()
        else transactionManager.suspendTransaction {
            AppRatingSnapshots.batchInsert(payloads) { p ->
                this[AppRatingSnapshots.store] = p.store
                this[AppRatingSnapshots.storeAppId] = p.storeAppId
                this[AppRatingSnapshots.country] = p.country
                this[AppRatingSnapshots.name] = p.name
                this[AppRatingSnapshots.ratingCount] = p.ratingCount
                this[AppRatingSnapshots.averageRating] = p.averageRating
                this[AppRatingSnapshots.capturedAt] = p.capturedAt
            }.map { AppRatingSnapshots.toSnapshot(it) }
        }

    override suspend fun listForApp(
        store: Store,
        storeAppId: String,
        country: String,
        pagination: Pagination,
    ): List<AppRatingSnapshot> =
        transactionManager.suspendTransaction {
            AppRatingSnapshots.selectAll()
                .where {
                    (AppRatingSnapshots.store eq store) and
                            (AppRatingSnapshots.storeAppId eq storeAppId) and
                            (AppRatingSnapshots.country eq country)
                }
                .orderBy(AppRatingSnapshots.capturedAt to SortOrder.DESC)
                .limit(pagination.limit.toInt()).offset(pagination.offset)
                .map { AppRatingSnapshots.toSnapshot(it) }
        }

    override suspend fun getLatestForApp(
        store: Store,
        storeAppId: String,
        country: String,
    ): AppRatingSnapshot? =
        transactionManager.suspendTransaction {
            AppRatingSnapshots.selectAll()
                .where {
                    (AppRatingSnapshots.store eq store) and
                            (AppRatingSnapshots.storeAppId eq storeAppId) and
                            (AppRatingSnapshots.country eq country)
                }
                .orderBy(AppRatingSnapshots.capturedAt to SortOrder.DESC)
                .limit(1)
                .map { AppRatingSnapshots.toSnapshot(it) }
                .firstOrNull()
        }

}
