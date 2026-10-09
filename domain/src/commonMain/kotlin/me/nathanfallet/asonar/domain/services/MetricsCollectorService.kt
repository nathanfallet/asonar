package me.nathanfallet.asonar.domain.services

import me.nathanfallet.asonar.domain.models.apps.Store
import kotlin.time.Duration

/**
 * Business metrics the use cases report. The domain says *what* happened; the infrastructure decides
 * where it goes (OpenTelemetry today).
 */
interface MetricsCollectorService {

    /**
     * One keyword fetch, and what it actually refreshed. Both flags false means the data was still
     * fresh and nothing went out to the stores.
     */
    fun recordKeywordFetch(store: Store, refreshedRanking: Boolean, refreshedPopularity: Boolean, duration: Duration)

    /**
     * One popularity read. [found] false is the signal to watch: on the App Store it is almost always
     * the Apple Search Ads session that expired, and every keyword then keeps a stale popularity.
     */
    fun recordPopularityRead(store: Store, found: Boolean)

}
