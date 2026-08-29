package me.nathanfallet.asonar.infrastructure.scraping

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import me.nathanfallet.asonar.api.Serialization
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.reviews.FetchedReview
import me.nathanfallet.asonar.domain.services.AppReviewSource
import org.slf4j.LoggerFactory
import kotlin.time.Instant

/**
 * [AppReviewSource] for the App Store, reading Apple's public customer-reviews RSS feed as JSON — a
 * plain HTTPS GET, **no browser and no authentication**. The country is in the URL path, so each
 * storefront serves its own reviews.
 *
 * Verified against the live feed (2026-08-29):
 * - **50 reviews per page, pages 1–10** (`page=11` answers HTTP 400) → 500 reviews max per market,
 *   which is why [maxPages] is 10.
 * - `sortBy=mostRecent` really is newest-first, which is what the incremental walk depends on.
 * - Coverage is **partial**: a real share of apps return a feed with no `entry` at all in a given
 *   market (measured: Jow/FR empty on repeated tries, while Good Pizza/FR and Instagram/US serve a
 *   full page). An empty feed is therefore a normal answer, not an error — the orchestrator treats it
 *   as "nothing more here" and moves on.
 *
 * The payload is navigated as raw JSON rather than mapped to strict `@Serializable` classes: Apple
 * wraps every scalar in a `{"label": …}` object, keys carry a namespace (`im:rating`), and `entry`
 * is an **object** when a market has a single review but an **array** otherwise. Navigating handles
 * that without a bespoke deserializer, and an entry that doesn't parse is skipped rather than
 * sinking the page.
 */
class AppStoreReviewSource(
    private val httpClient: HttpClient,
) : AppReviewSource {

    private val logger = LoggerFactory.getLogger(AppStoreReviewSource::class.java)

    override val store = Store.APP_STORE
    override val maxPages = MAX_PAGES

    override suspend fun getReviews(storeAppId: String, country: String, page: Int): List<FetchedReview> {
        if (page !in 1..MAX_PAGES) return emptyList()
        val body = fetchPage(storeAppId, country, page) ?: return emptyList()
        val entries = runCatching { entriesOf(body) }.getOrElse { error ->
            logger.warn("Unreadable review feed for app $storeAppId ($country) page $page: ${error.message}")
            return emptyList()
        }
        return entries.mapNotNull { it.toReview() }
    }

    /** The feed's reviews, tolerating the single-review shape and the "no reviews at all" shape. */
    private fun entriesOf(body: String): List<JsonObject> {
        val feed = Serialization.json.parseToJsonElement(body).jsonObject["feed"]?.jsonObject ?: return emptyList()
        return when (val entry = feed["entry"]) {
            is JsonArray -> entry.mapNotNull { it as? JsonObject }
            is JsonObject -> listOf(entry)
            else -> emptyList() // absent: this app has no reviews in this market
        }
    }

    private fun JsonObject.toReview(): FetchedReview? {
        val externalId = label("id") ?: return null
        val content = label("content") ?: return null
        val postedAt = label("updated")?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
        return FetchedReview(
            externalId = externalId,
            content = content,
            postedAt = postedAt,
            author = this["author"]?.jsonObject?.label("name"),
            title = label("title"),
            rating = label("im:rating")?.toIntOrNull(),
            version = label("im:version"),
        )
    }

    /** Apple wraps every scalar as `{"label": "…"}`. */
    private fun JsonObject.label(key: String): String? =
        (this[key] as? JsonObject)?.get("label")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

    /**
     * GET one page, retrying transient failures like the subtitle source does — a run walks several
     * markets back-to-back and Apple throttles bursts. Returns null only if every attempt failed;
     * that is logged, because "we couldn't read it" must stay distinguishable from "no reviews here".
     */
    private suspend fun fetchPage(storeAppId: String, country: String, page: Int): String? {
        val url = "https://itunes.apple.com/${country.lowercase()}/rss/customerreviews" +
                "/page=$page/id=$storeAppId/sortBy=mostRecent/json"
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            val result = runCatching {
                httpClient.get(url) {
                    header(HttpHeaders.Accept, "application/json")
                    // The shared client installs no decompressor, so ask for an uncompressed body.
                    header(HttpHeaders.AcceptEncoding, "identity")
                }.bodyAsText()
            }
            result.getOrNull()?.let { return it }
            lastError = result.exceptionOrNull()
            if (attempt < MAX_ATTEMPTS - 1) delay(RETRY_DELAY_MS)
        }
        logger.warn(
            "Review fetch failed for app $storeAppId ($country) page $page after $MAX_ATTEMPTS attempts",
            lastError
        )
        return null
    }

    companion object {
        /** Apple serves pages 1–10 (50 reviews each); page 11 answers HTTP 400. */
        private const val MAX_PAGES = 10
        private const val MAX_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 400L
    }

}
