package me.nathanfallet.asonar.infrastructure.database.repositories

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.snapshots.RankSnapshot
import me.nathanfallet.asonar.domain.models.snapshots.RankSnapshotPayload
import me.nathanfallet.asonar.domain.repositories.RankSnapshotsRepository
import me.nathanfallet.asonar.infrastructure.database.TransactionManager
import me.nathanfallet.asonar.infrastructure.database.tables.RankSnapshots
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
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

}
