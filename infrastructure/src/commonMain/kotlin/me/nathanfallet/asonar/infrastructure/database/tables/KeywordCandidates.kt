package me.nathanfallet.asonar.infrastructure.database.tables

import me.nathanfallet.asonar.domain.models.keywords.CandidateSource
import me.nathanfallet.asonar.domain.models.keywords.CandidateStatus
import me.nathanfallet.asonar.domain.models.keywords.KeywordCandidate
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * Terms discovery proposed for an app, awaiting a call. Unlike the snapshot tables this one is
 * mutable state, not history: a row is the *current* standing of a candidate (which sources found
 * it, whether we took it or dismissed it).
 */
object KeywordCandidates : LongIdTable() {

    val appId = long("app_id")
    val term = varchar("term", 255)
    val country = varchar("country", 2)

    /**
     * The [CandidateSource]s that proposed this term, as their comma-separated names. A set rather
     * than one value: agreement between sources is a relevance signal worth keeping. Stored inline
     * because it is at most a handful of names, only ever read with the row, and never queried on.
     */
    val sources = varchar("sources", 255)
    val detail = text("detail").nullable()
    val popularity = integer("popularity").nullable()
    val status = enumerationByName("status", 20, CandidateStatus::class)
    val discoveredAt = timestamp("discovered_at")
    val updatedAt = timestamp("updated_at")

    init {
        // A term is proposed once per app and market — re-discovery merges into that row.
        uniqueIndex(appId, term, country)
        // The review UI reads "this app's candidates in this state".
        index(false, appId, status)
    }

    fun toCandidate(row: ResultRow) = KeywordCandidate(
        row[id].value,
        row[appId],
        row[term],
        row[country],
        parseSources(row[sources]),
        row[detail],
        row[popularity],
        row[status],
        row[discoveredAt],
        row[updatedAt],
    )

    fun formatSources(values: Set<CandidateSource>): String = values.joinToString(",") { it.name }

    /**
     * Tolerant on read: a name that no longer maps to a [CandidateSource] (a source we dropped) is
     * skipped rather than blowing up the whole listing.
     */
    fun parseSources(raw: String): Set<CandidateSource> =
        raw.split(",")
            .mapNotNull { name -> CandidateSource.entries.firstOrNull { it.name == name.trim() } }
            .toSet()

}
