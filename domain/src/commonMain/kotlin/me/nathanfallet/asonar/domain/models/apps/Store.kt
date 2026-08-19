package me.nathanfallet.asonar.domain.models.apps

import kotlinx.serialization.Serializable

/** Which app store a keyword or app lives on. Its name is what gets stored in the database. */
@Serializable
enum class Store {
    APP_STORE,
    PLAY_STORE,
}
