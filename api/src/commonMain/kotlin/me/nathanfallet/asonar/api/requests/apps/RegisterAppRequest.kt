package me.nathanfallet.asonar.api.requests.apps

import kotlinx.serialization.Serializable

/** Body to register an app to track. [store] is the store name (e.g. "APP_STORE"). */
@Serializable
data class RegisterAppRequest(
    val store: String,
    val storeAppId: String,
    val name: String,
)
