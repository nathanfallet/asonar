package me.nathanfallet.asonar.infrastructure.database.repositories

import me.nathanfallet.asonar.api.Serialization
import me.nathanfallet.asonar.domain.models.keywords.KeywordSignals
import me.nathanfallet.asonar.domain.models.keywords.KeywordSignalsPayload
import me.nathanfallet.asonar.domain.repositories.KeywordSignalsRepository
import me.nathanfallet.asonar.infrastructure.database.TransactionManager
import me.nathanfallet.asonar.infrastructure.database.tables.KeywordSignalSnapshots
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

class KeywordSignalsDatabaseRepository(
    private val transactionManager: TransactionManager,
) : KeywordSignalsRepository {

    init {
        transactionManager.transaction {
            SchemaUtils.create(KeywordSignalSnapshots)
        }
    }

    override suspend fun create(payload: KeywordSignalsPayload): KeywordSignals =
        transactionManager.suspendTransaction {
            val newId = KeywordSignalSnapshots.insertAndGetId {
                it[keywordId] = payload.keywordId
                it[competitors] = Serialization.json.encodeToString(payload.competitors)
                it[totalResults] = payload.totalResults
                it[capturedAt] = payload.capturedAt
            }.value
            KeywordSignals(
                newId,
                payload.keywordId,
                payload.competitors,
                payload.totalResults,
                payload.capturedAt,
            )
        }

    override suspend fun getLatestForKeyword(keywordId: Long): KeywordSignals? =
        transactionManager.suspendTransaction {
            KeywordSignalSnapshots.selectAll()
                .where { KeywordSignalSnapshots.keywordId eq keywordId }
                .orderBy(KeywordSignalSnapshots.capturedAt to SortOrder.DESC)
                .limit(1)
                .map { KeywordSignalSnapshots.toSignals(it) }
                .firstOrNull()
        }

    // Latest signals per keyword via a join to a "MAX(captured_at) per keyword_id" derived table — reads
    // only the latest row of each keyword through the (keyword_id, captured_at) index, in one query, and
    // stays cheap as the history grows (see docs/database-optimization.md).
    override suspend fun latestByKeyword(): Map<Long, KeywordSignals> =
        transactionManager.suspendTransaction {
            val maxAt = KeywordSignalSnapshots.capturedAt.max().alias("max_at")
            val latest = KeywordSignalSnapshots
                .select(KeywordSignalSnapshots.keywordId, maxAt)
                .groupBy(KeywordSignalSnapshots.keywordId)
                .alias("latest")
            KeywordSignalSnapshots
                .join(
                    latest, JoinType.INNER,
                    onColumn = KeywordSignalSnapshots.keywordId,
                    otherColumn = latest[KeywordSignalSnapshots.keywordId],
                    additionalConstraint = { KeywordSignalSnapshots.capturedAt eq latest[maxAt] },
                )
                .selectAll()
                .associate { it[KeywordSignalSnapshots.keywordId] to KeywordSignalSnapshots.toSignals(it) }
        }

}
