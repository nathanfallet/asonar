package me.nathanfallet.asonar.domain.usecases.apps

import me.nathanfallet.asonar.domain.models.apps.AppKeywordCoverage

/** Builds the keyword-ranking coverage of one of our apps. */
interface GetAppKeywordCoverageUseCase {

    /** The app's coverage across every keyword tracked on its store, or null if the app is unknown. */
    suspend operator fun invoke(appId: Long): AppKeywordCoverage?

}
