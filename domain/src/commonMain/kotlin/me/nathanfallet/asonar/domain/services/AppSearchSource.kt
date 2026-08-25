package me.nathanfallet.asonar.domain.services

import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.search.KeywordSearchResult

/**
 * A source of ranked search results for a keyword on one [store] — the raw material from which the
 * orchestrator derives the top-of-results and our apps' ranks. One implementation per store.
 */
interface AppSearchSource {

    /** Which store this source covers. */
    val store: Store

    /** Searches the store for [term] in [country], returning up to [limit] results in rank order. */
    suspend fun search(term: String, country: String, limit: Int): KeywordSearchResult

}
