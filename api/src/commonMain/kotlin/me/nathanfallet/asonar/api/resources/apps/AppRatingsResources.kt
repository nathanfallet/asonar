package me.nathanfallet.asonar.api.resources.apps

import io.ktor.resources.*
import kotlinx.serialization.Serializable

/**
 * An app's ratings history in a market — keyed by store + store id + country, independent of any
 * keyword (the data is shared across every keyword the app appears in).
 */
@Serializable
@Resource("/api/app-ratings")
class AppRatingsApi(
    val store: String,
    val storeAppId: String,
    val country: String,
)
