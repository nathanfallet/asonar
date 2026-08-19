package me.nathanfallet.asonar.infrastructure.database.tables

import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshot
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object PopularitySnapshots : LongIdTable() {

    val keywordId = long("keyword_id")
    val popularity = integer("popularity")
    val capturedAt = timestamp("captured_at")

    init {
        // Every read is "the history / the latest for this keyword", newest first.
        index(false, keywordId, capturedAt)
    }

    fun toSnapshot(row: ResultRow) = PopularitySnapshot(
        row[id].value,
        row[keywordId],
        row[popularity],
        row[capturedAt],
    )

}
