package me.nathanfallet.asonar.domain.usecases.apps

import me.nathanfallet.asonar.domain.models.apps.App
import me.nathanfallet.asonar.domain.models.apps.AppPayload
import me.nathanfallet.asonar.domain.repositories.AppsRepository

class GetOrCreateAppUseCaseImpl(
    private val appsRepository: AppsRepository,
) : GetOrCreateAppUseCase {

    override suspend fun invoke(payload: AppPayload): App =
        appsRepository.getByStoreAppId(payload.store, payload.storeAppId)
            ?: appsRepository.create(payload)

}
