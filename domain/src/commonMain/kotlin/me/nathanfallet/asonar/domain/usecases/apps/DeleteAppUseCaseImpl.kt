package me.nathanfallet.asonar.domain.usecases.apps

import me.nathanfallet.asonar.domain.repositories.AppsRepository

class DeleteAppUseCaseImpl(
    private val appsRepository: AppsRepository,
) : DeleteAppUseCase {

    override suspend fun invoke(id: Long): Boolean = appsRepository.delete(id)

}
