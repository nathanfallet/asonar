package me.nathanfallet.asonar.infrastructure.database.repositories

import me.nathanfallet.asonar.domain.models.snapshots.TopAppSnapshot
import me.nathanfallet.asonar.domain.models.snapshots.TopAppSnapshotPayload
import me.nathanfallet.asonar.domain.repositories.TopAppSnapshotsRepository
import me.nathanfallet.asonar.infrastructure.database.TransactionManager
import me.nathanfallet.asonar.infrastructure.database.tables.TopAppSnapshots
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll

class TopAppSnapshotsDatabaseRepository(
    private val transactionManager: TransactionManager,
) : TopAppSnapshotsRepository {

    init {
        transactionManager.transaction {
            SchemaUtils.create(TopAppSnapshots)
        }
    }

    override suspend fun create(payload: TopAppSnapshotPayload): TopAppSnapshot =
        transactionManager.suspendTransaction {
            val newId = TopAppSnapshots.insertAndGetId {
                it[keywordId] = payload.keywordId
                it[position] = payload.position
                it[storeAppId] = payload.storeAppId
                it[appName] = payload.appName
                it[capturedAt] = payload.capturedAt
            }.value
            TopAppSnapshot(
                newId,
                payload.keywordId,
                payload.position,
                payload.storeAppId,
                payload.appName,
                payload.capturedAt,
            )
        }

    override suspend fun listLatestForKeyword(keywordId: Long): List<TopAppSnapshot> =
        transactionManager.suspendTransaction {
            // The latest observation is the rows sharing the most recent capturedAt for this keyword.
            val latest = TopAppSnapshots.selectAll()
                .where { TopAppSnapshots.keywordId eq keywordId }
                .orderBy(TopAppSnapshots.capturedAt to SortOrder.DESC)
                .limit(1)
                .map { it[TopAppSnapshots.capturedAt] }
                .firstOrNull() ?: return@suspendTransaction emptyList()

            TopAppSnapshots.selectAll()
                .where {
                    (TopAppSnapshots.keywordId eq keywordId) and (TopAppSnapshots.capturedAt eq latest)
                }
                .orderBy(TopAppSnapshots.position to SortOrder.ASC)
                .map { TopAppSnapshots.toSnapshot(it) }
        }

}
