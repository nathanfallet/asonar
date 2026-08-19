package me.nathanfallet.asonar.client

import me.nathanfallet.asonar.client.api.keywords.KeywordsApiClient

/**
 * The asonar API client: a facade exposing one sub-client per resource, so callers write
 * `client.keywords.getAll()`. Construct it with [ApiClientImpl] and a base URL.
 */
interface ApiClient {

    /** The keywords endpoints. */
    val keywords: KeywordsApiClient

}
