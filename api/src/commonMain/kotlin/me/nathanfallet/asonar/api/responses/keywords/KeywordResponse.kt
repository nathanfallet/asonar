package me.nathanfallet.asonar.api.responses.keywords

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * A tracked keyword with its latest popularity reading, as sent over the wire. [store] is the store
 * name (e.g. "APP_STORE"), keeping this contract free of any domain type.
 */
@Serializable
data class KeywordResponse(
    val id: Long,
    val term: String,
    val store: String,
    val country: String,
    val createdAt: Instant,
    val latestPopularity: Int? = null,
    val latestPopularityAt: Instant? = null,
)
