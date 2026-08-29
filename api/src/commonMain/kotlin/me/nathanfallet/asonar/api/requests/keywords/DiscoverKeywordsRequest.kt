package me.nathanfallet.asonar.api.requests.keywords

import kotlinx.serialization.Serializable

/**
 * Body to run a discovery pass. Both fields are optional: [countries] defaults to every market the
 * app already tracks keywords in, and [seeds] to its best-measured tracked terms in each market.
 * Sources expand *from* seeds — an unseeded pass returns store top-charts, not related terms.
 */
@Serializable
data class DiscoverKeywordsRequest(
    val countries: List<String>? = null,
    val seeds: List<String>? = null,
)

/** Body to act on reviewed candidates: [accept] starts tracking them, [dismiss] buries them. */
@Serializable
data class ReviewKeywordCandidatesRequest(
    val accept: List<Long> = emptyList(),
    val dismiss: List<Long> = emptyList(),
)
