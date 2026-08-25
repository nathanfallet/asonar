package me.nathanfallet.asonar.domain.usecases.apps

import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.snapshots.AppRatingHistory

/** Reads an app's ratings history for a store and market, with the ratings-per-day velocity. */
interface GetAppRatingHistoryUseCase {

    suspend operator fun invoke(store: Store, storeAppId: String, country: String): AppRatingHistory

}
