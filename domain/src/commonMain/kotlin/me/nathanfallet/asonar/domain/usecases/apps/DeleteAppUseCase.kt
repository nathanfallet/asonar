package me.nathanfallet.asonar.domain.usecases.apps

/** Removes an app. */
interface DeleteAppUseCase {

    /** @return True if an app was deleted. */
    suspend operator fun invoke(id: Long): Boolean

}
