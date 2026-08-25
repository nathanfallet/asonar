package me.nathanfallet.asonar.infrastructure.scraping

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Live verification (hits the real App Store product page). Proves the plain-HTTP fetch + subtitle
 * extraction works and that the URL's country actually drives the storefront — the FR and US
 * subtitles of the same app must differ.
 */
class AppStoreSubtitleSourceTest {

    @Test
    fun fetchesLocalizedSubtitle() = runBlocking {
        val source = AppStoreSubtitleSource(HttpClient(CIO))
        val fr = source.getSubtitle("911121200", "FR") // Good Pizza, Great Pizza
        val us = source.getSubtitle("911121200", "US")
        println("[test] FR subtitle = $fr")
        println("[test] US subtitle = $us")
        assertNotNull(fr, "FR subtitle should be found")
        assertNotNull(us, "US subtitle should be found")
        assertTrue(fr.isNotBlank() && us.isNotBlank())
        assertTrue(fr != us, "subtitle must be localized to the storefront in the URL")
    }

}
