package me.nathanfallet.asonar.client

import me.nathanfallet.asonar.client.api.apps.AppsApiClient
import me.nathanfallet.asonar.client.api.keywords.KeywordsApiClient

/**
 * The asonar API client: a facade exposing one sub-client per resource, so callers write
 * `client.keywords.getAll()`. Construct it with [ApiClientImpl] and a base URL.
 */
interface ApiClient {

    /** The apps endpoints. */
    val apps: AppsApiClient

    /** The keywords endpoints. */
    val keywords: KeywordsApiClient

}
