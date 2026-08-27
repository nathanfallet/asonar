package me.nathanfallet.asonar.infrastructure.database.repositories

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.snapshots.RankSnapshot
import me.nathanfallet.asonar.domain.models.snapshots.RankSnapshotPayload
import me.nathanfallet.asonar.domain.repositories.RankSnapshotsRepository
import me.nathanfallet.asonar.infrastructure.database.TransactionManager
import me.nathanfallet.asonar.infrastructure.database.tables.RankSnapshots
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

class RankSnapshotsDatabaseRepository(
    private val transactionManager: TransactionManager,
) : RankSnapshotsRepository {

    init {
        transactionManager.transaction {
            SchemaUtils.create(RankSnapshots)
        }
    }

    override suspend fun create(payload: RankSnapshotPayload): RankSnapshot =
        transactionManager.suspendTransaction {
            val newId = RankSnapshots.insertAndGetId {
                it[keywordId] = payload.keywordId
                it[appId] = payload.appId
                it[rank] = payload.rank
                it[totalResults] = payload.totalResults
                it[capturedAt] = payload.capturedAt
            }.value
            RankSnapshot(
                newId,
                payload.keywordId,
                payload.appId,
                payload.rank,
                payload.totalResults,
                payload.capturedAt,
            )
        }

    override suspend fun createAll(payloads: List<RankSnapshotPayload>): List<RankSnapshot> =
        if (payloads.isEmpty()) emptyList()
        else transactionManager.suspendTransaction {
            RankSnapshots.batchInsert(payloads) { p ->
                this[RankSnapshots.keywordId] = p.keywordId
                this[RankSnapshots.appId] = p.appId
                this[RankSnapshots.rank] = p.rank
                this[RankSnapshots.totalResults] = p.totalResults
                this[RankSnapshots.capturedAt] = p.capturedAt
            }.map { RankSnapshots.toSnapshot(it) }
        }

    override suspend fun listForKeywordAndApp(
        keywordId: Long,
        appId: Long,
        pagination: Pagination,
    ): List<RankSnapshot> =
        transactionManager.suspendTransaction {
            RankSnapshots.selectAll()
                .where { (RankSnapshots.keywordId eq keywordId) and (RankSnapshots.appId eq appId) }
                .orderBy(RankSnapshots.capturedAt to SortOrder.DESC)
                .limit(pagination.limit.toInt()).offset(pagination.offset)
                .map { RankSnapshots.toSnapshot(it) }
        }

    override suspend fun getLatestForKeywordAndApp(keywordId: Long, appId: Long): RankSnapshot? =
        transactionManager.suspendTransaction {
            RankSnapshots.selectAll()
                .where { (RankSnapshots.keywordId eq keywordId) and (RankSnapshots.appId eq appId) }
                .orderBy(RankSnapshots.capturedAt to SortOrder.DESC)
                .limit(1)
                .map { RankSnapshots.toSnapshot(it) }
                .firstOrNull()
        }

    // Latest rank of the app per keyword via a join to a "MAX(captured_at) per keyword_id for this app"
    // derived table — reads only the app's latest row per keyword through the (keyword_id, app_id,
    // captured_at) index, in one query (the coverage/opportunities N+1 — see docs/database-optimization.md).
    override suspend fun latestByKeywordForApp(appId: Long): Map<Long, RankSnapshot> =
        transactionManager.suspendTransaction {
            val maxAt = RankSnapshots.capturedAt.max().alias("max_at")
            val latest = RankSnapshots
                .select(RankSnapshots.keywordId, maxAt)
                .where { RankSnapshots.appId eq appId }
                .groupBy(RankSnapshots.keywordId)
                .alias("latest")
            RankSnapshots
                .join(
                    latest, JoinType.INNER,
                    onColumn = RankSnapshots.keywordId,
                    otherColumn = latest[RankSnapshots.keywordId],
                    additionalConstraint = {
                        (RankSnapshots.capturedAt eq latest[maxAt]) and (RankSnapshots.appId eq appId)
                    },
                )
                .selectAll()
                .associate { it[RankSnapshots.keywordId] to RankSnapshots.toSnapshot(it) }
        }

    override suspend fun historyByKeywordForApp(appId: Long): Map<Long, List<RankSnapshot>> =
        transactionManager.suspendTransaction {
            RankSnapshots.selectAll()
                .where { RankSnapshots.appId eq appId }
                .orderBy(RankSnapshots.capturedAt to SortOrder.DESC)
                .map { RankSnapshots.toSnapshot(it) }
                .groupBy { it.keywordId }
        }

}
