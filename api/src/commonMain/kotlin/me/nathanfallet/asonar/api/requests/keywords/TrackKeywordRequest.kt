package me.nathanfallet.asonar.api.requests.keywords

import kotlinx.serialization.Serializable

/** Body to start tracking a keyword. [store] is the store name (e.g. "APP_STORE"). */
@Serializable
data class TrackKeywordRequest(
    val term: String,
    val store: String,
    val country: String,
)
