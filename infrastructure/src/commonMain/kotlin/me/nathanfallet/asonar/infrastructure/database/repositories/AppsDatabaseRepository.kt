package me.nathanfallet.asonar.infrastructure.database.repositories

import me.nathanfallet.asonar.domain.models.apps.App
import me.nathanfallet.asonar.domain.models.apps.AppPayload
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.repositories.AppsRepository
import me.nathanfallet.asonar.infrastructure.database.TransactionManager
import me.nathanfallet.asonar.infrastructure.database.tables.Apps
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Clock

class AppsDatabaseRepository(
    private val transactionManager: TransactionManager,
) : AppsRepository {

    init {
        transactionManager.transaction {
            SchemaUtils.create(Apps)
        }
    }

    override suspend fun list(): List<App> =
        transactionManager.suspendTransaction {
            Apps.selectAll()
                .orderBy(Apps.createdAt to SortOrder.DESC)
                .map { Apps.toApp(it) }
        }

    override suspend fun get(id: Long): App? =
        transactionManager.suspendTransaction {
            Apps.selectAll()
                .where { Apps.id eq id }
                .map { Apps.toApp(it) }
                .firstOrNull()
        }

    override suspend fun getByStoreAppId(store: Store, storeAppId: String): App? =
        transactionManager.suspendTransaction {
            Apps.selectAll()
                .where { (Apps.store eq store) and (Apps.storeAppId eq storeAppId) }
                .map { Apps.toApp(it) }
                .firstOrNull()
        }

    override suspend fun create(payload: AppPayload): App =
        transactionManager.suspendTransaction {
            val now = Clock.System.now()
            val newId = Apps.insertAndGetId {
                it[store] = payload.store
                it[storeAppId] = payload.storeAppId
                it[name] = payload.name
                it[createdAt] = now
            }.value
            App(newId, payload.store, payload.storeAppId, payload.name, now)
        }

    override suspend fun delete(id: Long): Boolean =
        transactionManager.suspendTransaction {
            Apps.deleteWhere { Apps.id eq id } == 1
        }

}
