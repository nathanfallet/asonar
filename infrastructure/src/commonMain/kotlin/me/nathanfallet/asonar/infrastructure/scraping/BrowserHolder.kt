package me.nathanfallet.asonar.infrastructure.scraping

import dev.kdriver.cdp.domain.target
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
 *
 * **Session persistence.** The profile dir survives on disk, but Apple's auth cookies
 * (`myacinfo`, `app-ads.sid`…) are *session* cookies: Chrome purges them on a clean exit unless it
 * restores the previous session on the next launch. `--restore-last-session` (see [createBrowser])
 * flips that switch, so a login done once by hand survives us stopping and re-running the server —
 * no re-login on every restart during development.
 *
 * TODO (later — passkey auto-login): even with session-restore, the session eventually expires and a
 * human has to re-authenticate. The robust fix is to drive Apple's passkey login from code. kdriver
 * exposes the full CDP **WebAuthn** domain (`dev.kdriver.cdp.domain.WebAuthn`:
 * `webAuthn.enable()` → `addVirtualAuthenticator(...)` → `addCredential(...)` /
 * `setUserVerified(true)` / `setAutomaticPresenceSimulation(true)`). The native macOS Touch ID
 * prompt itself can't be scripted, but a virtual authenticator seeded once with a credential
 * registered against the Apple ID could satisfy the WebAuthn challenge headlessly. To investigate.
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
                Config(
                    userDataDir = Path(profileDir),
                    headless = false,
                    expert = true,
                    // Restore the previous session on launch so Apple's session cookies (and thus the
                    // interactive login) aren't purged when we stop and restart. See the class doc.
                    browserArgs = listOf("--restore-last-session"),
                ),
            ).also { browser = it }
            currentBrowser.get(baseUrl).also {
                tab = it
                closeStrayTabs(it)
            }
        }
        awaitBaseUrl(currentTab)
        block(currentTab)
    }

    /**
     * Collapses the window back to the single tab we drive. `--restore-last-session` reopens every
     * tab from the previous run and `get()` adds a fresh one on top, so without this the tab count
     * climbs by one on every restart. The extras are throwaway — the Apple login lives in the
     * profile, not in any tab — so we close every page target except ours. Best-effort: a target
     * that refuses to close is left as-is rather than failing the fetch.
     */
    private suspend fun closeStrayTabs(keep: Tab) {
        val keepId = keep.targetId ?: return
        runCatching {
            keep.target.getTargets().targetInfos
                .filter { it.type == "page" && it.targetId != keepId }
                .forEach { stray -> runCatching { keep.target.closeTarget(stray.targetId) } }
        }
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
