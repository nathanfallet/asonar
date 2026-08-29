package me.nathanfallet.asonar.domain.models.apps

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * An app we optimize and follow across the keyword rankings. This is the app-factory reuse point:
 * every project registers the store id asonar should track its rank for. [role] separates the apps
 * we optimize from the competitors we merely watch.
 */
@Serializable
data class App(
    val id: Long,
    val store: Store,
    val storeAppId: String, // Apple adamId, or the Google Play package name
    val name: String,
    val role: AppRole,
    val createdAt: Instant,
)

/** What it takes to register an [App]. */
@Serializable
data class AppPayload(
    val store: Store,
    val storeAppId: String,
    val name: String,
    val role: AppRole = AppRole.OWNED,
)
