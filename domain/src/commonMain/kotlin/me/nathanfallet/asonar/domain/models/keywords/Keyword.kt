package me.nathanfallet.asonar.domain.models.keywords

import kotlinx.serialization.Serializable
import me.nathanfallet.asonar.domain.models.apps.Store
import kotlin.time.Instant

/**
 * A search term we track, on a given store and market. The (term, store, country) triple is the
 * identity: the same word in another country is another row, because its popularity differs there.
 */
@Serializable
data class Keyword(
    val id: Long,
    val term: String,
    val store: Store,
    val country: String, // ISO 3166-1 alpha-2, e.g. "FR"
    val createdAt: Instant,
)

/** What it takes to start tracking a [Keyword]. */
@Serializable
data class KeywordPayload(
    val term: String,
    val store: Store,
    val country: String,
)
