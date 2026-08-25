package me.nathanfallet.asonar.infrastructure.scraping

import dev.kdriver.core.browser.Browser
import dev.kdriver.core.browser.Config
import dev.kdriver.core.browser.createBrowser
import dev.kdriver.core.tab.Tab
import dev.kdriver.core.tab.evaluate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.files.Path

/**
 * Owns a single long-lived Chrome driven by kdriver, on a **persistent profile** so an interactive
 * login — done once by hand in the window that opens — is reused across runs. The browser is
 * started lazily on first use and left on a tab open at [baseUrl] (same origin as the requests we
 * replay). Every access is serialized by a mutex, so page-context fetches never overlap: polite,
 * one-at-a-time scraping. kdriver installs its own JVM shutdown hook to close the browser on exit.
 *
 * `expert = true` adds `--disable-web-security`, so a page-context `fetch` to Apple's API subdomains
 * isn't blocked by CORS.
 */
class BrowserHolder(
    private val scope: CoroutineScope,
    private val profileDir: String,
    private val baseUrl: String,
) {

    private val mutex = Mutex()
    private var browser: Browser? = null
    private var tab: Tab? = null

    /** Runs [block] on the shared tab (loaded at [baseUrl]), exclusively. */
    suspend fun <T> onPage(block: suspend (Tab) -> T): T = mutex.withLock {
        val currentTab = tab ?: run {
            val currentBrowser = browser ?: createBrowser(
                scope,
                Config(userDataDir = Path(profileDir), headless = false, expert = true),
            ).also { browser = it }
            currentBrowser.get(baseUrl).also { tab = it }
        }
        awaitBaseUrl(currentTab)
        block(currentTab)
    }

    /**
     * Waits until the tab is actually on [baseUrl] and finished loading — `get()` can hand back the
     * tab before the navigation settles, and a page-context fetch fired on `about:blank` fails
     * cross-origin. Gives up after a few seconds and lets the caller try anyway.
     */
    private suspend fun awaitBaseUrl(tab: Tab) {
        repeat(40) {
            val href = runCatching { tab.evaluate<String>("location.href") }.getOrNull().orEmpty()
            val ready = runCatching { tab.evaluate<String>("document.readyState") }.getOrNull().orEmpty()
            if (href.startsWith(baseUrl) && ready == "complete") return
            delay(500)
        }
    }

    suspend fun stop() = mutex.withLock {
        browser?.stop()
        browser = null
        tab = null
    }

}
