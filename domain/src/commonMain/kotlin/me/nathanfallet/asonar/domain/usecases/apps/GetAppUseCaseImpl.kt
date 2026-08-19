package me.nathanfallet.asonar.domain.usecases.apps

import me.nathanfallet.asonar.domain.models.apps.App
import me.nathanfallet.asonar.domain.repositories.AppsRepository

class GetAppUseCaseImpl(
    private val appsRepository: AppsRepository,
) : GetAppUseCase {

    override suspend fun invoke(id: Long): App? = appsRepository.get(id)

}
