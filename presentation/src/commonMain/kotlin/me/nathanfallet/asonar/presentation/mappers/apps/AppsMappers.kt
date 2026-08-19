package me.nathanfallet.asonar.presentation.mappers.apps

import me.nathanfallet.asonar.api.responses.apps.AppResponse
import me.nathanfallet.asonar.domain.models.apps.App

/** Maps an [App] to its wire form. */
fun App.toAppResponse() = AppResponse(
    id = id,
    store = store.name,
    storeAppId = storeAppId,
    name = name,
    createdAt = createdAt,
)
