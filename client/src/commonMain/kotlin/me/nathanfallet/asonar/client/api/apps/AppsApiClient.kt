package me.nathanfallet.asonar.client.api.apps

import me.nathanfallet.asonar.api.requests.apps.RegisterAppRequest
import me.nathanfallet.asonar.api.responses.apps.AppRatingHistoryResponse
import me.nathanfallet.asonar.api.responses.apps.AppResponse
import me.nathanfallet.asonar.api.responses.apps.AppsResponse

/** Client for the app endpoints. */
interface AppsApiClient {

    /** Lists the apps we optimize. */
    suspend fun getAll(): AppsResponse

    /** Reads one app by its id. */
    suspend fun get(id: Long): AppResponse

    /** Registers an app to follow (idempotent). */
    suspend fun register(request: RegisterAppRequest): AppResponse

    /** Stops following an app. */
    suspend fun delete(id: Long)

    /** Reads an app's ratings history in a market (with the ratings-per-day velocity). */
    suspend fun ratings(store: String, storeAppId: String, country: String): AppRatingHistoryResponse

}
