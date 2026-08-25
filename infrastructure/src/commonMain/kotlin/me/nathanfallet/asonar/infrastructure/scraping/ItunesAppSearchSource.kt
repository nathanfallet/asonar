package me.nathanfallet.asonar.infrastructure.scraping

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import me.nathanfallet.asonar.api.Serialization
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.search.KeywordSearchResult
import me.nathanfallet.asonar.domain.models.search.SearchResultApp
import me.nathanfallet.asonar.domain.services.AppSearchSource

/**
 * [AppSearchSource] for the App Store, backed by Apple's public iTunes Search API (no auth). The
 * result order approximates the store's search ranking — a consistent, trackable signal; if it
 * drifts too far from real ranks we swap in a store-search source later.
 */
class ItunesAppSearchSource(
    private val httpClient: HttpClient,
) : AppSearchSource {

    override val store = Store.APP_STORE

    override suspend fun search(term: String, country: String, limit: Int): KeywordSearchResult {
        // iTunes replies with content-type text/javascript, so parse the raw body ourselves.
        val body = httpClient.get("https://itunes.apple.com/search") {
            parameter("term", term)
            parameter("country", country.lowercase())
            parameter("entity", "software")
            parameter("limit", limit.coerceIn(1, 200))
        }.bodyAsText()
        val response = Serialization.json.decodeFromString<ItunesSearchResponse>(body)
        return KeywordSearchResult(
            totalResults = response.resultCount,
            apps = response.results.map { SearchResultApp(it.trackId.toString(), it.trackName) },
        )
    }

}

@Serializable
private data class ItunesSearchResponse(
    val resultCount: Int,
    val results: List<ItunesApp>,
)

@Serializable
private data class ItunesApp(
    val trackId: Long,
    val trackName: String,
)
