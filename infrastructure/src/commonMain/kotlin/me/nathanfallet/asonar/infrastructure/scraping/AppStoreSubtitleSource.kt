package me.nathanfallet.asonar.infrastructure.scraping

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.services.AppSubtitleSource
import org.slf4j.LoggerFactory

/**
 * [AppSubtitleSource] for the App Store. The subtitle is exposed by no catalog API (the iTunes
 * lookup/search endpoints omit it), so we read the public product page — a plain HTTPS GET, **no
 * browser**. The country is in the URL path (`/{country}/app/id{adamId}`), so the storefront serves
 * its own localized subtitle (verified: `fr` → « Le simulateur de Pizzeria », `us` → "Pizza
 * Business Simulator"). Apple renders the header subtitle as the single `<p class="subtitle …">`
 * element on the page; we extract exactly that. Best-effort: any failure (network, layout change,
 * unknown app) yields null rather than throwing.
 */
class AppStoreSubtitleSource(
    private val httpClient: HttpClient,
) : AppSubtitleSource {

    private val logger = LoggerFactory.getLogger(AppStoreSubtitleSource::class.java)

    override val store = Store.APP_STORE

    override suspend fun getSubtitle(storeAppId: String, country: String): String? {
        val html = fetchPage(storeAppId, country) ?: return null
        return SUBTITLE.find(html)?.groupValues?.get(1)?.let(::unescape)?.trim()?.takeIf { it.isNotEmpty() }
    }

    /**
     * GET the product page, retrying transient failures. Apple throttles bursts (a keyword fetch pulls
     * ~10 of these back-to-back), so a first attempt occasionally drops; a short backoff recovers it.
     * Returns null only if every attempt fails.
     */
    private suspend fun fetchPage(storeAppId: String, country: String): String? {
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            val result = runCatching {
                httpClient.get("https://apps.apple.com/${country.lowercase()}/app/id$storeAppId") {
                    header(HttpHeaders.UserAgent, USER_AGENT)
                    header(HttpHeaders.Accept, "text/html")
                    // The shared client installs no decompressor, so ask for an uncompressed body.
                    header(HttpHeaders.AcceptEncoding, "identity")
                }.bodyAsText()
            }
            result.getOrNull()?.let { return it }
            lastError = result.exceptionOrNull()
            if (attempt < MAX_ATTEMPTS - 1) delay(RETRY_DELAY_MS)
        }
        // Don't swallow it: a failed fetch (network, rate-limit, layout change…) must be visible so
        // we can tell "app has no subtitle" apart from "we couldn't read it".
        logger.warn("Subtitle fetch failed for app $storeAppId ($country) after $MAX_ATTEMPTS attempts", lastError)
        return null
    }

    /** Minimal HTML-entity decode for the handful that show up in subtitles. */
    private fun unescape(text: String): String = text
        .replace("&amp;", "&")
        .replace("&#39;", "'").replace("&apos;", "'")
        .replace("&quot;", "\"")
        .replace("&lt;", "<").replace("&gt;", ">")

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 400L

        /** The product-header subtitle: the only `<p class="subtitle …">` on an App Store page. */
        private val SUBTITLE = Regex("""<p class="subtitle[^"]*">([^<]*)</p>""")
        private const val USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 " +
                    "(KHTML, like Gecko) Version/17.0 Safari/605.1.15"
    }

}
