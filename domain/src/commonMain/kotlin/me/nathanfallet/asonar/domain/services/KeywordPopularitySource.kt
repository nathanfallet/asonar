package me.nathanfallet.asonar.domain.services

import me.nathanfallet.asonar.domain.models.apps.Store

/**
 * A source of keyword popularity (the 0–100 index) for one [store]. The orchestrator picks the
 * source whose [store] matches the keyword. Add a store = add an implementation, nothing else.
 */
interface KeywordPopularitySource {

    /** Which store this source covers. */
    val store: Store

    /** Popularity 0–100 for a term in a market, or null if unavailable. */
    suspend fun getPopularity(term: String, country: String): Int?

}
