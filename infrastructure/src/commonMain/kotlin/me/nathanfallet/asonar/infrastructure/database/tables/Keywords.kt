package me.nathanfallet.asonar.infrastructure.database.tables

import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.keywords.Keyword
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object Keywords : LongIdTable() {

    val term = varchar("term", 255)
    val store = enumerationByName("store", 20, Store::class)
    val country = varchar("country", 2)
    val createdAt = timestamp("created_at")

    init {
        // A term is tracked once per store and market.
        uniqueIndex(term, store, country)
    }

    fun toKeyword(row: ResultRow) = Keyword(
        row[id].value,
        row[term],
        row[store],
        row[country],
        row[createdAt],
    )

}
