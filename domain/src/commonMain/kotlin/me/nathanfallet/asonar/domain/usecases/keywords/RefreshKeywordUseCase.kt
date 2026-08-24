package me.nathanfallet.asonar.domain.usecases.keywords

/**
 * Triggers a fetch of a keyword's popularity/rank data: enqueues the job and returns immediately.
 * The actual fetching happens asynchronously in the consumer — nothing here writes fetched data.
 */
interface RefreshKeywordUseCase {

    /** @return True if the keyword exists and a fetch was queued; false if it was not found. */
    suspend operator fun invoke(keywordId: Long): Boolean

}
