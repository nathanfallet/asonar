package me.nathanfallet.asonar.infrastructure.database.tables

import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.snapshots.AppRatingSnapshot
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object AppRatingSnapshots : LongIdTable() {

    val store = enumerationByName("store", 20, Store::class)
    val storeAppId = varchar("store_app_id", 255)
    val country = varchar("country", 2)
    val name = text("name")
    val ratingCount = integer("rating_count").nullable()
    val averageRating = double("average_rating").nullable()
    val capturedAt = timestamp("captured_at")

    init {
        // Reads are "this app's ratings history in this market", newest first — shared across keywords.
        index(false, store, storeAppId, country, capturedAt)
    }

    fun toSnapshot(row: ResultRow) = AppRatingSnapshot(
        row[id].value,
        row[store],
        row[storeAppId],
        row[country],
        row[name],
        row[ratingCount],
        row[averageRating],
        row[capturedAt],
    )

}
