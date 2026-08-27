package me.nathanfallet.asonar.domain.repositories

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.snapshots.AppRatingSnapshot
import me.nathanfallet.asonar.domain.models.snapshots.AppRatingSnapshotPayload

/** Records and reads the ratings history of the apps seen in search results. */
interface AppRatingSnapshotsRepository {

    /** Records one app-rating reading. */
    suspend fun create(payload: AppRatingSnapshotPayload): AppRatingSnapshot

    /** Records many readings in a single batch insert (one statement, one transaction). */
    suspend fun createAll(payloads: List<AppRatingSnapshotPayload>): List<AppRatingSnapshot>

    /** Lists an app's ratings history for a store and market, newest first. */
    suspend fun listForApp(
        store: Store,
        storeAppId: String,
        country: String,
        pagination: Pagination,
    ): List<AppRatingSnapshot>

    /** Reads an app's most recent ratings for a store and market. */
    suspend fun getLatestForApp(store: Store, storeAppId: String, country: String): AppRatingSnapshot?

}
