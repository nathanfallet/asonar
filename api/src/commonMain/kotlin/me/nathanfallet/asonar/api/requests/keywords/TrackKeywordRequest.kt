package me.nathanfallet.asonar.api.requests.keywords

import kotlinx.serialization.Serializable

/**
 * Body to start tracking a keyword. [store] is the store name (e.g. "APP_STORE").
 *
 * Tracking a keyword the first time automatically queues an initial fetch of its data (popularity +
 * ranking), so a freshly-added keyword is never left empty — no separate refresh call is needed.
 */
@Serializable
data class TrackKeywordRequest(
    val term: String,
    val store: String,
    val country: String,
)
