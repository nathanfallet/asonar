package me.nathanfallet.asonar.domain.models.search

/**
 * One term a [me.nathanfallet.asonar.domain.services.KeywordSuggestionSource] proposes.
 *
 * [popularity] is the 0–100 search index when the source already knows it — Apple Search Ads returns
 * it with its recommendations, which is what makes that source worth far more than the others: a
 * candidate that floors at 5 can be dropped before it ever costs a fetch. Sources that only see text
 * (competitor metadata, reviews) leave it null and the term is measured later.
 *
 * [detail] is a human-readable trace of where it came from (the seed term, the competitor's store
 * id…), shown in the review UI so a candidate can be judged without digging.
 */
data class KeywordSuggestion(
    val term: String,
    val popularity: Int? = null,
    val detail: String? = null,
)
