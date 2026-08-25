package me.nathanfallet.asonar.domain.models.search

/** The ranked results of searching a store for a keyword — the raw material for rank + top-of-results. */
data class KeywordSearchResult(
    val totalResults: Int?,
    val apps: List<SearchResultApp>,
)

/** One app in the search results (in rank order). */
data class SearchResultApp(
    val storeAppId: String,
    val name: String,
)
