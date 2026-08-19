package me.nathanfallet.asonar.infrastructure.database.tables

import me.nathanfallet.asonar.domain.models.snapshots.RankSnapshot
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object RankSnapshots : LongIdTable() {

    val keywordId = long("keyword_id")
    val appId = long("app_id")
    val rank = integer("rank").nullable()
    val totalResults = integer("total_results").nullable()
    val capturedAt = timestamp("captured_at")

    init {
        // Every read is "the history / the latest for this app on this keyword", newest first.
        index(false, keywordId, appId, capturedAt)
    }

    fun toSnapshot(row: ResultRow) = RankSnapshot(
        row[id].value,
        row[keywordId],
        row[appId],
        row[rank],
        row[totalResults],
        row[capturedAt],
    )

}
