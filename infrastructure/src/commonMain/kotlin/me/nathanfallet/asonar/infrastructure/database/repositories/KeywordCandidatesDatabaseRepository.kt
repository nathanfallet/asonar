package me.nathanfallet.asonar.infrastructure.database.repositories

import me.nathanfallet.asonar.domain.models.keywords.CandidateStatus
import me.nathanfallet.asonar.domain.models.keywords.KeywordCandidate
import me.nathanfallet.asonar.domain.models.keywords.KeywordCandidatePayload
import me.nathanfallet.asonar.domain.repositories.KeywordCandidateUpsert
import me.nathanfallet.asonar.domain.repositories.KeywordCandidatesRepository
import me.nathanfallet.asonar.infrastructure.database.TransactionManager
import me.nathanfallet.asonar.infrastructure.database.tables.KeywordCandidates
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.*
import kotlin.time.Clock
import kotlin.time.Instant

class KeywordCandidatesDatabaseRepository(
    private val transactionManager: TransactionManager,
) : KeywordCandidatesRepository {

    init {
        transactionManager.transaction {
            SchemaUtils.create(KeywordCandidates)
        }
    }

    override suspend fun list(appId: Long, statuses: Set<CandidateStatus>): List<KeywordCandidate> =
        transactionManager.suspendTransaction {
            KeywordCandidates.selectAll()
                .where {
                    if (statuses.isEmpty()) KeywordCandidates.appId eq appId
                    else (KeywordCandidates.appId eq appId) and (KeywordCandidates.status inList statuses.toList())
                }
                // Known volume first (SQL sorts NULLs last on DESC in MySQL only by accident, so the
                // final ordering is settled in memory), then alphabetically for a stable list.
                .map { KeywordCandidates.toCandidate(it) }
                .sortedWith(compareByDescending<KeywordCandidate> { it.popularity ?: -1 }.thenBy { it.term })
        }

    override suspend fun get(id: Long): KeywordCandidate? =
        transactionManager.suspendTransaction {
            KeywordCandidates.selectAll()
                .where { KeywordCandidates.id eq id }
                .map { KeywordCandidates.toCandidate(it) }
                .firstOrNull()
        }

    override suspend fun upsertAll(payloads: List<KeywordCandidatePayload>): KeywordCandidateUpsert {
        if (payloads.isEmpty()) return KeywordCandidateUpsert(emptyList(), emptyList())
        return transactionManager.suspendTransaction {
            val now = Clock.System.now()
            // A run can legitimately propose the same term twice (two sources, or two seeds landing on
            // it) — fold those together first so the batch insert can't violate the unique index.
            val merged = payloads.fold(LinkedHashMap<Key, MutableList<KeywordCandidatePayload>>()) { acc, p ->
                acc.getOrPut(Key(p.appId, p.term.trim(), p.country.trim().uppercase())) { mutableListOf() }.add(p)
                acc
            }

            // One read for the whole run: everything already known for the apps it touches. Discovery
            // proposes hundreds of terms, so a per-term SELECT would be the same N+1 the read paths
            // already got rid of.
            val existing = KeywordCandidates.selectAll()
                .where { KeywordCandidates.appId inList merged.keys.map { it.appId }.distinct() }
                .map { KeywordCandidates.toCandidate(it) }
                .associateBy { Key(it.appId, it.term, it.country) }

            val (known, fresh) = merged.entries.partition { existing.containsKey(it.key) }

            val created = if (fresh.isEmpty()) emptyList() else KeywordCandidates.batchInsert(fresh) { (key, group) ->
                this[KeywordCandidates.appId] = key.appId
                this[KeywordCandidates.term] = key.term
                this[KeywordCandidates.country] = key.country
                this[KeywordCandidates.sources] = KeywordCandidates.formatSources(group.map { it.source }.toSet())
                this[KeywordCandidates.detail] = group.firstNotNullOfOrNull { it.detail }
                this[KeywordCandidates.popularity] = group.firstNotNullOfOrNull { it.popularity }
                this[KeywordCandidates.status] = CandidateStatus.NEW
                this[KeywordCandidates.discoveredAt] = now
                this[KeywordCandidates.updatedAt] = now
            }.map { KeywordCandidates.toCandidate(it) }

            // Merge into what's already there. The status is deliberately NOT touched: a dismissed term
            // stays dismissed however many times discovery finds it again — that's the whole point of
            // persisting candidates.
            val updated = known.map { (key, group) ->
                val row = existing.getValue(key)
                val sources = row.sources + group.map { it.source }
                val popularity = group.firstNotNullOfOrNull { it.popularity } ?: row.popularity
                val detail = group.firstNotNullOfOrNull { it.detail } ?: row.detail
                KeywordCandidates.update({ KeywordCandidates.id eq row.id }) {
                    it[KeywordCandidates.sources] = KeywordCandidates.formatSources(sources)
                    it[KeywordCandidates.detail] = detail
                    it[KeywordCandidates.popularity] = popularity
                    it[updatedAt] = now
                }
                row.copy(sources = sources, detail = detail, popularity = popularity, updatedAt = now)
            }

            KeywordCandidateUpsert(created, updated)
        }
    }

    override suspend fun updateStatus(ids: List<Long>, status: CandidateStatus): Int =
        if (ids.isEmpty()) 0
        else transactionManager.suspendTransaction {
            val now: Instant = Clock.System.now()
            KeywordCandidates.update({ KeywordCandidates.id inList ids }) {
                it[KeywordCandidates.status] = status
                it[updatedAt] = now
            }
        }

    override suspend fun deleteForApp(appId: Long): Int =
        transactionManager.suspendTransaction {
            KeywordCandidates.deleteWhere { KeywordCandidates.appId eq appId }
        }

    /** The (app, term, country) identity, normalized the way the table stores it. */
    private data class Key(val appId: Long, val term: String, val country: String)

}
