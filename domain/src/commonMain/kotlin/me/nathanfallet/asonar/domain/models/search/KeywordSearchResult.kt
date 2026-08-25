package me.nathanfallet.asonar.domain.models.search

/** The ranked results of searching a store for a keyword — the raw material for rank + top-of-results. */
data class KeywordSearchResult(
    val totalResults: Int?,
    val apps: List<SearchResultApp>,
)

/** One app in the search results (in rank order), with its ratings at the time of the search. */
data class SearchResultApp(
    val storeAppId: String,
    val name: String,
    val ratingCount: Int? = null,
    val averageRating: Double? = null,
)
