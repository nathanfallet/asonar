package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.keywords.Keyword

/**
 * Acts on reviewed candidates — the step that turns a proposal into tracked data (or buries it).
 *
 * Accepting starts tracking the term in its market (which queues its first fetch, like any new
 * keyword) and marks the candidate as taken. Dismissing is what makes persisting candidates worth it:
 * a rejected term stays rejected however many times discovery finds it again.
 */
interface ReviewKeywordCandidatesUseCase {

    /** Tracks the given candidates and marks them accepted. @return The resulting keywords. */
    suspend fun accept(ids: List<Long>): List<Keyword>

    /** Buries the given candidates so discovery stops proposing them. @return How many were buried. */
    suspend fun dismiss(ids: List<Long>): Int

}
