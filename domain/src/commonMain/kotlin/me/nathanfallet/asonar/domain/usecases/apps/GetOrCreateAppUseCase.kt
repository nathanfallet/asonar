package me.nathanfallet.asonar.domain.usecases.apps

import me.nathanfallet.asonar.domain.models.apps.App
import me.nathanfallet.asonar.domain.models.apps.AppPayload

/**
 * Returns the app for a store identity, registering it the first time it is seen. Idempotent: the
 * same (store, storeAppId) always resolves to the same row, so a scraper can hand it whatever app it
 * is about without checking first.
 */
interface GetOrCreateAppUseCase {

    suspend operator fun invoke(payload: AppPayload): App

}
