package me.nathanfallet.asonar.domain.usecases.runs

import me.nathanfallet.asonar.domain.models.runs.KeywordRunPayload
import me.nathanfallet.asonar.domain.models.runs.KeywordRunResult

/**
 * Records one fetch of a keyword: writes the popularity, our apps' ranks, and the top-of-results as
 * snapshots that all share the run's capturedAt. Append-only — a run never overwrites an earlier
 * one; it adds to the history.
 */
interface RecordKeywordRunUseCase {

    suspend operator fun invoke(payload: KeywordRunPayload): KeywordRunResult

}
