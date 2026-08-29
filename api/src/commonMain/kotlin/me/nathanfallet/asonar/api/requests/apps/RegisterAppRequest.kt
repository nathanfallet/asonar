package me.nathanfallet.asonar.api.requests.apps

import kotlinx.serialization.Serializable

/**
 * Body to register an app to track. [store] is the store name (e.g. "APP_STORE"); [role] is "OWNED"
 * (an app we optimize, the default) or "COMPETITOR" (one we watch).
 */
@Serializable
data class RegisterAppRequest(
    val store: String,
    val storeAppId: String,
    val name: String,
    val role: String? = null,
)
