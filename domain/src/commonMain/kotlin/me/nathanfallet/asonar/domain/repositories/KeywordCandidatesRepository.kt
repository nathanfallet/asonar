package me.nathanfallet.asonar.domain.repositories

import me.nathanfallet.asonar.domain.models.keywords.CandidateStatus
import me.nathanfallet.asonar.domain.models.keywords.KeywordCandidate
import me.nathanfallet.asonar.domain.models.keywords.KeywordCandidatePayload

/** Reads and writes the keyword candidates discovery proposed, per app. */
interface KeywordCandidatesRepository {

    /**
     * Lists an app's candidates, best first: known popularity descending (unknown last), then term.
     * [statuses] filters the review state; empty means every state.
     */
    suspend fun list(appId: Long, statuses: Set<CandidateStatus> = emptySet()): List<KeywordCandidate>

    /** Reads one candidate by its identifier. */
    suspend fun get(id: Long): KeywordCandidate?

    /**
     * Records a discovery run. Idempotent per (app, term, country): a term already proposed keeps its
     * row — and above all its [CandidateStatus] — while merging in the new source and any popularity
     * the run learned. So re-running discovery never resurrects a dismissed term.
     */
    suspend fun upsertAll(payloads: List<KeywordCandidatePayload>): KeywordCandidateUpsert

    /** Moves candidates to a review state. @return How many rows changed. */
    suspend fun updateStatus(ids: List<Long>, status: CandidateStatus): Int

    /** Removes every candidate of an app (used when the app itself goes away). */
    suspend fun deleteForApp(appId: Long): Int

}

/**
 * What a discovery run changed — [created] are the terms we had never seen for this app, [updated]
 * the ones that came back (a second source, or a popularity we now know). Split so a run can report
 * "12 new, 40 already known" without the caller re-reading the table.
 */
data class KeywordCandidateUpsert(
    val created: List<KeywordCandidate>,
    val updated: List<KeywordCandidate>,
)
