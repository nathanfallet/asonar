package me.nathanfallet.asonar.domain.usecases.apps

/**
 * Kicks off a background refresh of an app's **relevant** keywords — the ones we rank on (to keep the
 * rank graph moving) and the ones that are opportunities (YES / YES_BUT), skipping the walls (NO) and
 * the parked ones (RESERVE / UNKNOWN). Fire it when the app view is loaded (page or MCP): the fetch's
 * own age-gating keeps it cheap, since keywords fetched recently are skipped.
 */
interface RefreshAppKeywordsUseCase {

    /** @return the number of keywords enqueued for a refresh (null if the app is unknown). */
    suspend operator fun invoke(appId: Long): Int?

}
