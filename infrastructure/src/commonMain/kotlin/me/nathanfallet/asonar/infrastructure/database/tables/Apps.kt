package me.nathanfallet.asonar.infrastructure.database.tables

import me.nathanfallet.asonar.domain.models.apps.App
import me.nathanfallet.asonar.domain.models.apps.Store
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object Apps : LongIdTable() {

    val store = enumerationByName("store", 20, Store::class)
    val storeAppId = varchar("store_app_id", 255)
    val name = text("name")
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
        row[createdAt],
    )

}
