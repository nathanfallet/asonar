package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.keywords.CandidateStatus
import me.nathanfallet.asonar.domain.models.keywords.Keyword
import me.nathanfallet.asonar.domain.models.keywords.KeywordPayload
import me.nathanfallet.asonar.domain.repositories.AppsRepository
import me.nathanfallet.asonar.domain.repositories.KeywordCandidatesRepository

class ReviewKeywordCandidatesUseCaseImpl(
    private val appsRepository: AppsRepository,
    private val keywordCandidatesRepository: KeywordCandidatesRepository,
    private val getOrCreateKeywordUseCase: GetOrCreateKeywordUseCase,
) : ReviewKeywordCandidatesUseCase {

    override suspend fun accept(ids: List<Long>): List<Keyword> {
        val candidates = ids.mapNotNull { keywordCandidatesRepository.get(it) }
        // The candidate carries no store of its own — it belongs to an app, and that app's store is
        // the one the term will be tracked on.
        val storeByApp = candidates.map { it.appId }.distinct()
            .mapNotNull { appId -> appsRepository.get(appId)?.let { appId to it.store } }
            .toMap()

        val keywords = candidates.mapNotNull { candidate ->
            val store = storeByApp[candidate.appId] ?: return@mapNotNull null
            getOrCreateKeywordUseCase(KeywordPayload(candidate.term, store, candidate.country))
        }
        // Only mark what we could actually track: a candidate whose app vanished mid-review stays NEW
        // rather than claiming a keyword that was never created.
        keywordCandidatesRepository.updateStatus(
            candidates.filter { storeByApp.containsKey(it.appId) }.map { it.id },
            CandidateStatus.ADDED,
        )
        return keywords
    }

    override suspend fun dismiss(ids: List<Long>): Int =
        keywordCandidatesRepository.updateStatus(ids, CandidateStatus.DISMISSED)

}
