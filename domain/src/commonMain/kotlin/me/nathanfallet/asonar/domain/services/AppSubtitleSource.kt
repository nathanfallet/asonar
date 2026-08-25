package me.nathanfallet.asonar.domain.services

import me.nathanfallet.asonar.domain.models.apps.Store

/**
 * Fetches an app's localized **subtitle** (App Store) / **short description** (Play Store) for a
 * given market — the concise, keyword-optimized tagline that no official catalog API exposes.
 * Store-specific and selected by [store]. The value MUST come from the storefront matching the
 * requested country, never a random locale: a FR subtitle read against a US ranking would poison
 * the relevance analysis.
 */
interface AppSubtitleSource {

    val store: Store

    /** The app's subtitle in [country]'s storefront (ISO alpha-2), or null if it can't be read. */
    suspend fun getSubtitle(storeAppId: String, country: String): String?

}
