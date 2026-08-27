package me.nathanfallet.asonar.infrastructure.database.repositories

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshot
import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshotPayload
import me.nathanfallet.asonar.domain.repositories.PopularitySnapshotsRepository
import me.nathanfallet.asonar.infrastructure.database.TransactionManager
import me.nathanfallet.asonar.infrastructure.database.tables.PopularitySnapshots
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll

class PopularitySnapshotsDatabaseRepository(
    private val transactionManager: TransactionManager,
) : PopularitySnapshotsRepository {

    init {
        transactionManager.transaction {
            SchemaUtils.create(PopularitySnapshots)
        }
    }

    override suspend fun create(payload: PopularitySnapshotPayload): PopularitySnapshot =
        transactionManager.suspendTransaction {
            val newId = PopularitySnapshots.insertAndGetId {
                it[keywordId] = payload.keywordId
                it[popularity] = payload.popularity
                it[capturedAt] = payload.capturedAt
            }.value
            PopularitySnapshot(newId, payload.keywordId, payload.popularity, payload.capturedAt)
        }

    override suspend fun listForKeyword(
        keywordId: Long,
        pagination: Pagination,
    ): List<PopularitySnapshot> =
        transactionManager.suspendTransaction {
            PopularitySnapshots.selectAll()
                .where { PopularitySnapshots.keywordId eq keywordId }
                .orderBy(PopularitySnapshots.capturedAt to SortOrder.DESC)
                .limit(pagination.limit.toInt()).offset(pagination.offset)
                .map { PopularitySnapshots.toSnapshot(it) }
        }

    override suspend fun getLatestForKeyword(keywordId: Long): PopularitySnapshot? =
        transactionManager.suspendTransaction {
            PopularitySnapshots.selectAll()
                .where { PopularitySnapshots.keywordId eq keywordId }
                .orderBy(PopularitySnapshots.capturedAt to SortOrder.DESC)
                .limit(1)
                .map { PopularitySnapshots.toSnapshot(it) }
                .firstOrNull()
        }

    // One read + in-memory reduce instead of a getLatest per keyword (the N+1). A small history table,
    // so this stays cheap; move to a per-group SQL query if it ever grows large (see docs/database-optimization.md).
    override suspend fun latestByKeyword(): Map<Long, PopularitySnapshot> =
        transactionManager.suspendTransaction {
            PopularitySnapshots.selectAll()
                .map { PopularitySnapshots.toSnapshot(it) }
                .groupBy { it.keywordId }
                .mapValues { (_, rows) -> rows.maxBy { it.capturedAt } }
        }

}
