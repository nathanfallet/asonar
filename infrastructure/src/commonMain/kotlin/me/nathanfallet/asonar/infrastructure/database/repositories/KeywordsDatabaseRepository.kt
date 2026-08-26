package me.nathanfallet.asonar.infrastructure.database.repositories

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.keywords.Keyword
import me.nathanfallet.asonar.domain.models.keywords.KeywordPayload
import me.nathanfallet.asonar.domain.repositories.KeywordsRepository
import me.nathanfallet.asonar.infrastructure.database.TransactionManager
import me.nathanfallet.asonar.infrastructure.database.tables.Keywords
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Clock

class KeywordsDatabaseRepository(
    private val transactionManager: TransactionManager,
) : KeywordsRepository {

    init {
        transactionManager.transaction {
            SchemaUtils.create(Keywords)
        }
    }

    override suspend fun list(pagination: Pagination): List<Keyword> =
        transactionManager.suspendTransaction {
            Keywords.selectAll()
                .apply { pagination.search?.let { s -> andWhere { Keywords.term like "%$s%" } } }
                .orderBy(Keywords.createdAt to SortOrder.DESC)
                // limit <= 0 means "no limit" — a stopgap until real pagination (see ROADMAP): with a
                // few thousand keywords on a local instance, load them all rather than truncate.
                .apply { if (pagination.limit > 0) limit(pagination.limit.toInt()).offset(pagination.offset) }
                .map { Keywords.toKeyword(it) }
        }

    override suspend fun get(id: Long): Keyword? =
        transactionManager.suspendTransaction {
            Keywords.selectAll()
                .where { Keywords.id eq id }
                .map { Keywords.toKeyword(it) }
                .firstOrNull()
        }

    override suspend fun getByTerm(term: String, store: Store, country: String): Keyword? =
        transactionManager.suspendTransaction {
            Keywords.selectAll()
                .where {
                    (Keywords.term eq term) and (Keywords.store eq store) and (Keywords.country eq country)
                }
                .map { Keywords.toKeyword(it) }
                .firstOrNull()
        }

    override suspend fun create(payload: KeywordPayload): Keyword =
        transactionManager.suspendTransaction {
            val now = Clock.System.now()
            val newId = Keywords.insertAndGetId {
                it[term] = payload.term
                it[store] = payload.store
                it[country] = payload.country
                it[createdAt] = now
            }.value
            Keyword(newId, payload.term, payload.store, payload.country, now)
        }

    override suspend fun delete(id: Long): Boolean =
        transactionManager.suspendTransaction {
            Keywords.deleteWhere { Keywords.id eq id } == 1
        }

}
