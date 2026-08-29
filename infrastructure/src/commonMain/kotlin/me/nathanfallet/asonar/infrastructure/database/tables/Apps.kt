package me.nathanfallet.asonar.infrastructure.database.tables

import me.nathanfallet.asonar.domain.models.apps.App
import me.nathanfallet.asonar.domain.models.apps.AppRole
import me.nathanfallet.asonar.domain.models.apps.Store
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object Apps : LongIdTable() {

    val store = enumerationByName("store", 20, Store::class)
    val storeAppId = varchar("store_app_id", 255)
    val name = text("name")

    /**
     * Defaulted so an existing database keeps working after the column is added: every app registered
     * before roles existed was one of ours. ⚠️ `SchemaUtils.create` does NOT alter an existing table —
     * on a database that predates this column, run:
     * `ALTER TABLE Apps ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'OWNED';`
     */
    val role = enumerationByName("role", 20, AppRole::class).default(AppRole.OWNED)
    val createdAt = timestamp("created_at")

    init {
        // The same store id is only ever registered once.
        uniqueIndex(store, storeAppId)
    }

    fun toApp(row: ResultRow) = App(
        row[id].value,
        row[store],
        row[storeAppId],
        row[name],
        row[role],
        row[createdAt],
    )

}
