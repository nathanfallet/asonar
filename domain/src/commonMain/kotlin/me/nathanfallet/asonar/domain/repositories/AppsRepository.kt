package me.nathanfallet.asonar.domain.repositories

import me.nathanfallet.asonar.domain.models.apps.App
import me.nathanfallet.asonar.domain.models.apps.AppPayload
import me.nathanfallet.asonar.domain.models.apps.AppRole
import me.nathanfallet.asonar.domain.models.apps.Store

/** Reads and writes the apps we optimize. */
interface AppsRepository {

    /** Lists every tracked app, newest first. */
    suspend fun list(): List<App>

    /** Reads an app by its identifier. */
    suspend fun get(id: Long): App?

    /** Reads an app by its store identity, used to avoid registering the same one twice. */
    suspend fun getByStoreAppId(store: Store, storeAppId: String): App?

    /** Registers an app. */
    suspend fun create(payload: AppPayload): App

    /** Changes what an app is followed for. @return The updated app, or null if it doesn't exist. */
    suspend fun updateRole(id: Long, role: AppRole): App?

    /** Removes an app. @return True if a row was deleted. */
    suspend fun delete(id: Long): Boolean

}
