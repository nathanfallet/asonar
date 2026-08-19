package me.nathanfallet.asonar.domain.usecases.apps

import me.nathanfallet.asonar.domain.models.apps.App
import me.nathanfallet.asonar.domain.repositories.AppsRepository

class ListAppsUseCaseImpl(
    private val appsRepository: AppsRepository,
) : ListAppsUseCase {

    override suspend fun invoke(): List<App> = appsRepository.list()

}
