package me.nathanfallet.asonar.api.responses.keywords

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * One discovered term awaiting review. [sources] are the discovery sources that proposed it (several
 * means several angles agreed, which is a relevance signal); [status] is NEW / ADDED / DISMISSED.
 *
 * [popularity] is Apple's 0–100 search index when the source knew it — a candidate at 5 is at the
 * floor of the index (nobody searches it), whatever else it has going for it.
 */
@Serializable
data class KeywordCandidateResponse(
    val id: Long,
    val appId: Long,
    val term: String,
    val country: String,
    val sources: List<String>,
    val detail: String? = null,
    val popularity: Int? = null,
    val status: String,
    val discoveredAt: Instant,
    val updatedAt: Instant,
)

/** An app's candidates, best first (known popularity descending, unknown last). */
@Serializable
data class KeywordCandidatesResponse(
    val candidates: List<KeywordCandidateResponse>,
)

/** What a discovery pass changed: terms never seen before for this app, and ones it merged into. */
@Serializable
data class KeywordDiscoveryResponse(
    val created: List<KeywordCandidateResponse>,
    val updated: List<KeywordCandidateResponse>,
)
