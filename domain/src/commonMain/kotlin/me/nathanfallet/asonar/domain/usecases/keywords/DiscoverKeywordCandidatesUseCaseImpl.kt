package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.keywords.Keyword
import me.nathanfallet.asonar.domain.models.keywords.KeywordCandidatePayload
import me.nathanfallet.asonar.domain.repositories.*
import me.nathanfallet.asonar.domain.services.KeywordSuggestionSource

class DiscoverKeywordCandidatesUseCaseImpl(
    private val appsRepository: AppsRepository,
    private val keywordsRepository: KeywordsRepository,
    private val keywordCandidatesRepository: KeywordCandidatesRepository,
    private val popularitySnapshotsRepository: PopularitySnapshotsRepository,
    private val suggestionSources: List<KeywordSuggestionSource>,
) : DiscoverKeywordCandidatesUseCase {

    override suspend fun invoke(
        appId: Long,
        countries: List<String>?,
        seeds: List<String>?,
    ): KeywordCandidateUpsert? {
        val app = appsRepository.get(appId) ?: return null
        val sources = suggestionSources.filter { it.store == app.store }
        if (sources.isEmpty()) return KeywordCandidateUpsert(emptyList(), emptyList())

        val tracked = keywordsRepository.list(Pagination(limit = 0)).filter { it.store == app.store }
        val markets = countries?.map { it.trim().uppercase() }?.filter { it.isNotEmpty() }?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?: tracked.map { it.country }.distinct()

        // Terms already tracked in a market are not candidates — proposing what we measure already is
        // pure noise in the review list.
        val trackedByCountry = tracked.groupBy({ it.country }, { it.term })
        val popularityByKeyword = popularitySnapshotsRepository.latestByKeyword()

        val payloads = mutableListOf<KeywordCandidatePayload>()
        for (country in markets) {
            val marketSeeds = seeds?.map { it.trim().lowercase() }?.filter { it.isNotEmpty() }?.distinct()
                ?: defaultSeeds(tracked, country, popularityByKeyword)
            if (marketSeeds.isEmpty()) continue

            val alreadyTracked = trackedByCountry[country].orEmpty().toSet()
            for (source in sources) {
                source.suggest(app.storeAppId, country, marketSeeds)
                    .filterNot { it.term in alreadyTracked }
                    .forEach { suggestion ->
                        payloads += KeywordCandidatePayload(
                            appId = app.id,
                            term = suggestion.term,
                            country = country,
                            source = source.source,
                            detail = suggestion.detail,
                            popularity = suggestion.popularity,
                        )
                    }
            }
        }
        return keywordCandidatesRepository.upsertAll(payloads)
    }

    /**
     * Seeds from the keywords we already track in this market, the best-measured ones first. Rationale:
     * a source expands a term's neighbourhood, and the neighbourhood of a term with real volume is
     * where the volume is — seeding from a floored term returns floored terms. Capped because each seed
     * costs one source request, and they run one at a time through a single browser tab.
     */
    private fun defaultSeeds(
        tracked: List<Keyword>,
        country: String,
        popularityByKeyword: Map<Long, me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshot>,
    ): List<String> = tracked
        .filter { it.country == country }
        .sortedByDescending { popularityByKeyword[it.id]?.popularity ?: -1 }
        .take(MAX_SEEDS_PER_MARKET)
        .map { it.term }

    companion object {
        /** How many tracked terms we expand from per market when the caller doesn't pick the seeds. */
        internal const val MAX_SEEDS_PER_MARKET = 8
    }

}
