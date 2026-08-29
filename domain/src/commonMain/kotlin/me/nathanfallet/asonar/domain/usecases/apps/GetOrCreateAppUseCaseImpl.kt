package me.nathanfallet.asonar.domain.usecases.apps

import me.nathanfallet.asonar.domain.models.apps.App
import me.nathanfallet.asonar.domain.models.apps.AppPayload
import me.nathanfallet.asonar.domain.repositories.AppsRepository

class GetOrCreateAppUseCaseImpl(
    private val appsRepository: AppsRepository,
) : GetOrCreateAppUseCase {

    override suspend fun invoke(payload: AppPayload): App {
        val existing = appsRepository.getByStoreAppId(payload.store, payload.storeAppId)
            ?: return appsRepository.create(payload)
        // Registering a known app again is a no-op, EXCEPT for the role: "this one is a competitor"
        // (or "this one is mine now") has to land, otherwise the caller has no way to fix it.
        return if (existing.role == payload.role) existing
        else appsRepository.updateRole(existing.id, payload.role) ?: existing
    }

}
