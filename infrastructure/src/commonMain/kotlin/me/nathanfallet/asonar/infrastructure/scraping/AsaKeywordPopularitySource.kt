package me.nathanfallet.asonar.infrastructure.scraping

import dev.kdriver.core.tab.evaluate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import me.nathanfallet.asonar.api.Serialization
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.services.KeywordPopularitySource
import org.slf4j.LoggerFactory

/**
 * [KeywordPopularitySource] for the App Store. Popularity (the 0–100 index) comes from Apple Search
 * Ads' internal `getKeywordPopularities`, replayed through an authenticated kdriver browser session
 * (persistent Chrome profile, page-context `fetch`). Endpoint + query captured from a live session.
 *
 * The market is chosen per keyword: `storefronts` takes the country's ISO-3166 alpha-2 code (e.g.
 * "FR", "US"), so any tracked country works from a single logged-in session. Whenever anything fails
 * (not logged in, no data…) it returns null, which the orchestrator treats as "not fetched".
 */
class AsaKeywordPopularitySource(
    private val browserHolder: BrowserHolder,
    private val adamId: String,
    private val graphqlEndpoint: String,
) : KeywordPopularitySource {

    override val store = Store.APP_STORE

    private val logger = LoggerFactory.getLogger(AsaKeywordPopularitySource::class.java)

    override suspend fun getPopularity(term: String, country: String): Int? {
        return try {
            browserHolder.onPage { tab ->
                val script = readAsset("assets/asa/getKeywordPopularities.js")
                    .replace("{ENDPOINT}", graphqlEndpoint)
                    .replace("{ADAM_ID}", adamId)
                    .replace("{STOREFRONT}", country.trim().uppercase())
                    .replace("{TERM}", term.replace("\"", ""))
                val raw = tab.evaluate<JsonElement>(script, awaitPromise = true) ?: return@onPage null
                Serialization.json.decodeFromJsonElement<AsaResponse>(raw).popularityFor(term)
            }
        } catch (e: Exception) {
            logger.warn("[asa] popularity fetch failed for '$term' ($country): ${e.message}")
            null
        }
    }

    private fun readAsset(name: String): String =
        this::class.java.classLoader.getResource(name)?.readText()
            ?: error("Missing asset: $name")

    @Serializable
    private data class AsaResponse(val data: Data? = null) {

        @Serializable
        data class Data(val recommendationV2: Rec? = null)

        @Serializable
        data class Rec(val getKeywordPopularities: List<Pop> = emptyList())

        @Serializable
        data class Pop(val name: String? = null, val popularity: Int? = null)

        fun popularityFor(term: String): Int? {
            val list = data?.recommendationV2?.getKeywordPopularities ?: return null
            return (list.firstOrNull { it.name.equals(term, ignoreCase = true) } ?: list.firstOrNull())
                ?.popularity
        }
    }

}
