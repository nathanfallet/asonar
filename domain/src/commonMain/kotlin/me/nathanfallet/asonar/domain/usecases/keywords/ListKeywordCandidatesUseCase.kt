package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.keywords.CandidateStatus
import me.nathanfallet.asonar.domain.models.keywords.KeywordCandidate

/**
 * Lists an app's discovered keyword candidates for review — best first (known popularity descending,
 * unknown last). Filters are the two questions a reviewer actually asks: which ones are still
 * pending, and which ones have enough volume to be worth a slot.
 */
interface ListKeywordCandidatesUseCase {

    /**
     * @param appId The app whose candidates to list.
     * @param statuses Review states to keep. Empty = every state (default: the pending ones).
     * @param minPopularity Drops candidates whose known popularity is below this. Candidates with an
     *   unknown popularity are always kept — they haven't been measured, which is not the same as
     *   being small.
     * @return The candidates, or null if the app doesn't exist.
     */
    suspend operator fun invoke(
        appId: Long,
        statuses: Set<CandidateStatus> = setOf(CandidateStatus.NEW),
        minPopularity: Int? = null,
    ): List<KeywordCandidate>?

}
