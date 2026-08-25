package me.nathanfallet.asonar.api.responses.apps

import kotlinx.serialization.Serializable
import me.nathanfallet.asonar.api.responses.keywords.KeywordResponse
import kotlin.time.Instant

/** One point of an app's rank history on a keyword. A null [rank] means not ranked at that time. */
@Serializable
data class RankPointResponse(
    val rank: Int? = null,
    val capturedAt: Instant,
)

/** One tracked keyword and our app's standing on it: current rank (null = not ranked) + history. */
@Serializable
data class KeywordCoverageEntryResponse(
    val keyword: KeywordResponse,
    val popularity: Int? = null,
    val rank: Int? = null,
    val totalResults: Int? = null,
    val capturedAt: Instant? = null,
    val history: List<RankPointResponse> = emptyList(),
)

/** An app's ranking coverage across every keyword tracked on its store. */
@Serializable
data class AppKeywordCoverageResponse(
    val app: AppResponse,
    val entries: List<KeywordCoverageEntryResponse>,
)
