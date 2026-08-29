package me.nathanfallet.asonar.domain.services

import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.keywords.CandidateSource
import me.nathanfallet.asonar.domain.models.search.KeywordSuggestion

/**
 * A source of *candidate* terms for an app — the front of the funnel, upstream of everything else in
 * asonar. It answers "what else could this app rank on", never "is it worth it": scoring stays the
 * [me.nathanfallet.asonar.domain.usecases.keywords.OpportunityScorer]'s job, on tracked keywords.
 *
 * One implementation per (store, [source]); the orchestrator runs every source matching the app's
 * store and merges their proposals into candidates. A source that can't answer returns an empty list
 * rather than throwing — a dead source must not sink a whole discovery run.
 */
interface KeywordSuggestionSource {

    /** Which store this source covers. */
    val store: Store

    /** Which kind of candidate it produces (recorded on the candidate). */
    val source: CandidateSource

    /**
     * Proposes terms for [storeAppId] in [country]'s market. [seeds] are terms to expand from (the
     * keywords we already track, typically); a source that doesn't need seeds ignores them.
     */
    suspend fun suggest(storeAppId: String, country: String, seeds: List<String>): List<KeywordSuggestion>

}
