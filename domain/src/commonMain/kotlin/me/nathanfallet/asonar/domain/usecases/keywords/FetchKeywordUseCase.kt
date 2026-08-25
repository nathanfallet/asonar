package me.nathanfallet.asonar.domain.usecases.keywords

/**
 * Fetches a keyword's data from the store sources and records it as a run. This is the orchestrator
 * behind the fetch queue: it picks the right per-store sources, derives the top-of-results and our
 * apps' ranks, and hands the assembled run to [RecordKeywordRunUseCase]. Runs in the consumer.
 */
interface FetchKeywordUseCase {

    suspend operator fun invoke(keywordId: Long)

}
