package me.nathanfallet.asonar.client.api.keywords

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.resources.*
import me.nathanfallet.asonar.api.resources.keywords.KeywordsApi
import me.nathanfallet.asonar.api.responses.keywords.KeywordsResponse

/** [KeywordsApiClient] backed by the shared Ktor [HttpClient]. */
class KeywordsApiClientImpl(
    private val client: HttpClient,
) : KeywordsApiClient {

    override suspend fun getAll(): KeywordsResponse = client
        .get(KeywordsApi())
        .body()

}
