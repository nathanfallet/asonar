package me.nathanfallet.asonar.infrastructure.database.tables

import me.nathanfallet.asonar.api.Serialization
import me.nathanfallet.asonar.domain.models.keywords.CompetitorSignal
import me.nathanfallet.asonar.domain.models.keywords.KeywordSignals
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

/** Append-only keyword-level opportunity signals, precomputed at fetch time (Option B). */
object KeywordSignalSnapshots : LongIdTable() {

    val keywordId = long("keyword_id")

    // The captured top-of-results scoring inputs, as a JSON array of CompetitorSignal.
    val competitors = text("competitors")
    val totalResults = integer("total_results").nullable()
    val capturedAt = timestamp("captured_at")

    init {
        // Reads pull the newest signals for a keyword.
        index(false, keywordId, capturedAt)
    }

    fun toSignals(row: ResultRow) = KeywordSignals(
        row[id].value,
        row[keywordId],
        Serialization.json.decodeFromString<List<CompetitorSignal>>(row[competitors]),
        row[totalResults],
        row[capturedAt],
    )

}
