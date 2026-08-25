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
    /**
     * The app's subtitle (App Store) / short description (Play Store) — key for the keyword
     * recommender: knowing whether a competitor puts the term in its title *or* subtitle is half the
     * relevance signal. LIMITATION: **not populated yet.** The iTunes Search/Lookup API doesn't
     * return it; it has to be read from the store product page (a plain HTTPS GET of
     * `apps.apple.com/{country}/app/id{adamId}` carries it — no browser needed — and Play's listing
     * exposes the short description the same way). Wiring that source + persisting it is roadmap
     * item #1 (see docs/ROADMAP.md). Until then this stays null.
     */
    val subtitle: String? = null,
)
