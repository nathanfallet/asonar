package me.nathanfallet.asonar.domain.usecases.apps

import me.nathanfallet.asonar.domain.models.apps.App

/** Reads one app by its identifier. */
interface GetAppUseCase {

    suspend operator fun invoke(id: Long): App?

}
