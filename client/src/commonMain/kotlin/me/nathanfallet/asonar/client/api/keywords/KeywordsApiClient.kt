package me.nathanfallet.asonar.client.api.keywords

import me.nathanfallet.asonar.api.responses.keywords.KeywordsResponse

/** Client for the keywords endpoints. */
interface KeywordsApiClient {

    /** Retrieves the tracked keywords with their latest popularity. */
    suspend fun getAll(): KeywordsResponse

}
