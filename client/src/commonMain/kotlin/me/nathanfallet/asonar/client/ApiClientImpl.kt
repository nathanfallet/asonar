package me.nathanfallet.asonar.client

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import me.nathanfallet.asonar.api.Serialization
import me.nathanfallet.asonar.client.api.apps.AppsApiClientImpl
import me.nathanfallet.asonar.client.api.keywords.KeywordsApiClientImpl

/**
 * [ApiClient] backed by a Ktor [HttpClient].
 *
 * @param baseUrl Where asonar is reachable, applied to every request. A string rather than an
 *                environment enum, because a local deployment can live anywhere.
 * @param clientBuilder Builds the [HttpClient]; overridable so tests can inject a MockEngine.
 */
class ApiClientImpl(
    private val baseUrl: String,
    private val clientBuilder: (HttpClientConfig<out HttpClientEngineConfig>.() -> Unit) -> HttpClient = { config ->
        HttpClient(config)
    },
) : ApiClient {

    private val client = clientBuilder {
        expectSuccess = true
        install(Resources)
        install(ContentNegotiation) {
            json(Serialization.json)
        }
        defaultRequest {
            url(baseUrl)
        }
    }

    override val apps = AppsApiClientImpl(client)
    override val keywords = KeywordsApiClientImpl(client)

}
