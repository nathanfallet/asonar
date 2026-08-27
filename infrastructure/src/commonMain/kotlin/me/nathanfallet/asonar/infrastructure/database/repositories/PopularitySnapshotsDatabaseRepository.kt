package me.nathanfallet.asonar.infrastructure.database.repositories

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshot
import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshotPayload
import me.nathanfallet.asonar.domain.repositories.PopularitySnapshotsRepository
import me.nathanfallet.asonar.infrastructure.database.TransactionManager
import me.nathanfallet.asonar.infrastructure.database.tables.PopularitySnapshots
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
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

    // Latest row per keyword, without a getLatest per keyword (the N+1) nor a full-table read: join the
    // table to a "MAX(captured_at) per keyword_id" derived table. The GROUP BY reads only the
    // (keyword_id, captured_at) index (covering), and the join matches back on that same index — so it
    // reads just the latest row of each keyword, in one query, and stays cheap as the history grows.
    override suspend fun latestByKeyword(): Map<Long, PopularitySnapshot> =
        transactionManager.suspendTransaction {
            val maxAt = PopularitySnapshots.capturedAt.max().alias("max_at")
            val latest = PopularitySnapshots
                .select(PopularitySnapshots.keywordId, maxAt)
                .groupBy(PopularitySnapshots.keywordId)
                .alias("latest")
            PopularitySnapshots
                .join(
                    latest, JoinType.INNER,
                    onColumn = PopularitySnapshots.keywordId,
                    otherColumn = latest[PopularitySnapshots.keywordId],
                    additionalConstraint = { PopularitySnapshots.capturedAt eq latest[maxAt] },
                )
                .selectAll()
                .associate { it[PopularitySnapshots.keywordId] to PopularitySnapshots.toSnapshot(it) }
        }

}
