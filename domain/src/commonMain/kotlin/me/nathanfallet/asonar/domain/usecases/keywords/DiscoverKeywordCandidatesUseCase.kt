package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.repositories.KeywordCandidateUpsert

/**
 * Runs keyword discovery for an app: asks every source that covers its store for candidate terms and
 * merges the proposals into its candidate list. This is the front of the funnel — it proposes, it
 * never decides. Nothing is tracked until a human (or an agent) accepts a candidate.
 *
 * Discovery is seeded: sources expand *from* terms rather than inventing them (see
 * [me.nathanfallet.asonar.domain.services.KeywordSuggestionSource]). Left to itself the use case
 * seeds from the keywords already tracked in each market, best-measured first.
 */
interface DiscoverKeywordCandidatesUseCase {

    /**
     * @param appId The app to discover for.
     * @param countries Markets to run, ISO alpha-2. Null/empty = every market the app already tracks
     *   keywords in.
     * @param seeds Terms to expand from. Null/empty = the app's best tracked keywords in each market
     *   (capped — one source request per seed and per market).
     * @return What the run created and merged, or null if the app doesn't exist.
     */
    suspend operator fun invoke(
        appId: Long,
        countries: List<String>? = null,
        seeds: List<String>? = null,
    ): KeywordCandidateUpsert?

}
