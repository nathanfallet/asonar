package me.nathanfallet.asonar.domain.services

/**
 * Port for asking that a keyword be (re)fetched. The domain only knows "queue a fetch"; the
 * infrastructure decides how (it publishes to RabbitMQ, and a consumer runs the scrape later).
 */
interface KeywordFetchQueue {

    /** Enqueues a fetch for the given keyword. Returns immediately — the work runs asynchronously. */
    suspend fun enqueueFetch(keywordId: Long)

}
