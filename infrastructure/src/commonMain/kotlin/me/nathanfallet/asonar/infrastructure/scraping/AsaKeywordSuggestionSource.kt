package me.nathanfallet.asonar.infrastructure.scraping

import dev.kdriver.core.tab.evaluate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import me.nathanfallet.asonar.api.Serialization
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.keywords.CandidateSource
import me.nathanfallet.asonar.domain.models.search.KeywordSuggestion
import me.nathanfallet.asonar.domain.services.KeywordSuggestionSource
import org.slf4j.LoggerFactory

/**
 * [KeywordSuggestionSource] for the App Store: Apple Search Ads' own keyword recommendations, read
 * through the same authenticated kdriver session as [AsaKeywordPopularitySource] (persistent Chrome
 * profile, page-context `fetch`, internal `getRecommendedKeywordsGql` operation).
 *
 * **Why this source outranks every other one.** Apple returns each suggestion *with its popularity*,
 * so a candidate can be judged before it ever costs a fetch. Terms we invent almost all floor at 5
 * (a real measurement: 208 hand-guessed combinations, 100 % at the floor), because App Store search
 * volume sits on a handful of broad category heads, not on the vocabulary that describes an app best.
 * Apple does not suggest terms nobody searches.
 *
 * **Seeds are mandatory.** The `text` argument is nullable in the schema but useless when null —
 * Apple then answers with the store's biggest apps ("instagram", "snapchat"…) rather than anything
 * related to [adamId]. So a run without seeds returns nothing instead of flooding candidates with
 * top-charts noise. One request per (seed, country); the shared browser tab serializes them.
 *
 * Failures are per-seed: a seed that errors is logged and skipped, so one bad request can't sink a
 * whole discovery run.
 */
class AsaKeywordSuggestionSource(
    private val browserHolder: BrowserHolder,
    private val graphqlEndpoint: String,
) : KeywordSuggestionSource {

    override val store = Store.APP_STORE
    override val source = CandidateSource.ASA

    private val logger = LoggerFactory.getLogger(AsaKeywordSuggestionSource::class.java)

    override suspend fun suggest(
        storeAppId: String,
        country: String,
        seeds: List<String>,
    ): List<KeywordSuggestion> {
        if (seeds.isEmpty()) {
            logger.warn("[asa] no seed for $storeAppId ($country) — skipped (an unseeded call returns top charts)")
            return emptyList()
        }
        val script = readAsset("assets/asa/getRecommendedKeywords.js")
            .replace("{ENDPOINT}", graphqlEndpoint)
            .replace("{ADAM_ID}", storeAppId)
            .replace("{STOREFRONT}", country.trim().uppercase())

        // Merge across seeds, keeping the first popularity seen for a term: the same term often comes
        // back from several seeds, and a candidate is one row whatever proposed it.
        val merged = LinkedHashMap<String, KeywordSuggestion>()
        for (seed in seeds) {
            for (suggestion in suggest(script, seed, storeAppId, country)) {
                merged.putIfAbsent(suggestion.term, suggestion)
            }
        }
        return merged.values.toList()
    }

    private suspend fun suggest(
        script: String,
        seed: String,
        storeAppId: String,
        country: String,
    ): List<KeywordSuggestion> = try {
        browserHolder.onPage { tab ->
            val raw = tab.evaluate<JsonElement>(
                script.replace("{TEXT}", seed.replace("\"", "")),
                awaitPromise = true,
            ) ?: return@onPage emptyList()
            Serialization.json.decodeFromJsonElement<AsaResponse>(raw).suggestions(seed)
        }
    } catch (e: Exception) {
        logger.warn("[asa] recommendations failed for '$seed' ($storeAppId, $country): ${e.message}")
        emptyList()
    }

    private fun readAsset(name: String): String =
        this::class.java.classLoader.getResource(name)?.readText()
            ?: error("Missing asset: $name")

    @Serializable
    private data class AsaResponse(val data: Data? = null) {

        @Serializable
        data class Data(val recommendationV2: Rec? = null)

        @Serializable
        data class Rec(val getRecommendedKeywords: List<Keyword> = emptyList())

        @Serializable
        data class Keyword(val name: String? = null, val popularity: Int? = null)

        fun suggestions(seed: String): List<KeywordSuggestion> =
            data?.recommendationV2?.getRecommendedKeywords.orEmpty()
                .mapNotNull { keyword ->
                    val term = keyword.name?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                    // Apple echoes the seed back among its suggestions; we already track it.
                    if (term == seed.trim().lowercase()) return@mapNotNull null
                    KeywordSuggestion(term, keyword.popularity, "Apple Search Ads · seed « $seed »")
                }

    }

}
