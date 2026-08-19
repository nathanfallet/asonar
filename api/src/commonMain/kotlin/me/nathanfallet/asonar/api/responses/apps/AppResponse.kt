package me.nathanfallet.asonar.api.responses.apps

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** An app we optimize, as sent over the wire. [store] is the store name (e.g. "APP_STORE"). */
@Serializable
data class AppResponse(
    val id: Long,
    val store: String,
    val storeAppId: String,
    val name: String,
    val createdAt: Instant,
)

/** A list of apps. */
@Serializable
data class AppsResponse(
    val apps: List<AppResponse>,
)
