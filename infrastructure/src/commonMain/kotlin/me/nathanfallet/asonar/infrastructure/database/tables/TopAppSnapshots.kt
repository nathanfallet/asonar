package me.nathanfallet.asonar.infrastructure.database.tables

import me.nathanfallet.asonar.domain.models.snapshots.TopAppSnapshot
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object TopAppSnapshots : LongIdTable() {

    val keywordId = long("keyword_id")
    val position = integer("position")
    val storeAppId = varchar("store_app_id", 255)
    val appName = text("app_name")
    val capturedAt = timestamp("captured_at")

    init {
        // Reads pull one keyword's rows for a given observation, newest observation first.
        index(false, keywordId, capturedAt)
    }

    fun toSnapshot(row: ResultRow) = TopAppSnapshot(
        row[id].value,
        row[keywordId],
        row[position],
        row[storeAppId],
        row[appName],
        row[capturedAt],
    )

}
