package me.nathanfallet.asonar.domain.usecases.apps

import me.nathanfallet.asonar.domain.models.apps.App
import me.nathanfallet.asonar.domain.models.apps.AppPayload

/**
 * Returns the app for a store identity, registering it the first time it is seen. Idempotent: the
 * same (store, storeAppId) always resolves to the same row, so a scraper can hand it whatever app it
 * is about without checking first. Re-registering a known app with a different
 * [me.nathanfallet.asonar.domain.models.apps.AppRole] moves it to that role rather than silently
 * ignoring the change — that is the only way a registration mutates an existing row.
 */
interface GetOrCreateAppUseCase {

    suspend operator fun invoke(payload: AppPayload): App

}
