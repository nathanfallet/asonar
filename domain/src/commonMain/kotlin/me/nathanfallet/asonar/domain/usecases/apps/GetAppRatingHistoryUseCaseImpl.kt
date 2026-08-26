package me.nathanfallet.asonar.domain.usecases.apps

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.snapshots.AppRatingHistory
import me.nathanfallet.asonar.domain.models.snapshots.AppRatingSnapshot
import me.nathanfallet.asonar.domain.repositories.AppRatingSnapshotsRepository
import me.nathanfallet.asonar.domain.usecases.apps.GetAppRatingHistoryUseCaseImpl.Companion.ratingsPerDay
import kotlin.math.roundToInt

class GetAppRatingHistoryUseCaseImpl(
    private val appRatingSnapshotsRepository: AppRatingSnapshotsRepository,
) : GetAppRatingHistoryUseCase {

    override suspend fun invoke(store: Store, storeAppId: String, country: String): AppRatingHistory {
        val snapshots = appRatingSnapshotsRepository.listForApp(
            store = store,
            storeAppId = storeAppId,
            country = country,
            pagination = Pagination(limit = 500),
        )
        val latest = snapshots.firstOrNull() // newest first
        return AppRatingHistory(
            store = store,
            storeAppId = storeAppId,
            country = country,
            name = latest?.name,
            latestRatingCount = latest?.ratingCount,
            latestAverageRating = latest?.averageRating,
            ratingsPer30d = ratingsGained(snapshots),
            snapshots = snapshots,
        )
    }

    companion object {
        private const val MILLIS_PER_DAY = 86_400_000L

        /** The window the ASO velocity signal is expressed over. */
        private const val PROJECTION_DAYS = 30.0

        /**
         * Least-squares slope of ratingCount over time, in ratings per day. Null with under 2 readings,
         * or when the readings don't fall on **at least two distinct calendar days** (UTC) — several
         * points a few hours apart on the same day aren't a representative trend, extrapolating a daily
         * (let alone monthly) rate from them gives a wild, meaningless number. "Yesterday and today" is
         * the minimum; it fills in on its own as the history accrues over real days.
         */
        internal fun ratingsPerDay(snapshots: List<AppRatingSnapshot>): Double? {
            val points = snapshots.mapNotNull { snapshot -> snapshot.ratingCount?.let { it to snapshot.capturedAt } }
            if (points.size < 2) return null
            if (points.map { it.second.toEpochMilliseconds().floorDiv(MILLIS_PER_DAY) }.distinct().size < 2) return null
            val origin = points.minOf { it.second }
            val xs = points.map { (it.second - origin).inWholeMilliseconds / 86_400_000.0 } // days
            val ys = points.map { it.first.toDouble() }
            val meanX = xs.average()
            val meanY = ys.average()
            var numerator = 0.0
            var denominator = 0.0
            for (i in xs.indices) {
                val dx = xs[i] - meanX
                numerator += dx * (ys[i] - meanY)
                denominator += dx * dx
            }
            return if (denominator == 0.0) null else numerator / denominator
        }

        /**
         * New ratings gained over the last [days] days, taken from the trend but **never extrapolated
         * past the window we've actually observed**. An app first seen 10 days ago at 0 ratings and now
         * at 10 reads **+10**, not +30 — we refuse to invent a negative rating count before we had any
         * data on it. Only once the history covers [days] or more does this become the true per-[days]
         * rate. Null whenever the velocity itself is unknown (see [ratingsPerDay]).
         */
        internal fun ratingsGained(snapshots: List<AppRatingSnapshot>, days: Double = PROJECTION_DAYS): Int? {
            val slope = ratingsPerDay(snapshots) ?: return null
            return (slope * minOf(days, observedSpanDays(snapshots))).roundToInt()
        }

        /** How many days the (dated) readings actually cover, newest minus oldest. */
        private fun observedSpanDays(snapshots: List<AppRatingSnapshot>): Double {
            val times = snapshots.mapNotNull { snapshot -> snapshot.ratingCount?.let { snapshot.capturedAt } }
            if (times.size < 2) return 0.0
            return (times.max() - times.min()).inWholeMilliseconds / 86_400_000.0
        }
    }

}
