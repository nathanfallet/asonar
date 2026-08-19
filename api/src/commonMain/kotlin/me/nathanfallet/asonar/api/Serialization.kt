package me.nathanfallet.asonar.api

import kotlinx.serialization.json.Json

/** Shared JSON configuration for the API contract, used by both the server and the client. */
object Serialization {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
}
