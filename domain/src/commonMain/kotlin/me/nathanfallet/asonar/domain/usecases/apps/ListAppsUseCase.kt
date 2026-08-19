package me.nathanfallet.asonar.domain.usecases.apps

import me.nathanfallet.asonar.domain.models.apps.App

/** Lists the apps we optimize. */
interface ListAppsUseCase {

    suspend operator fun invoke(): List<App>

}
