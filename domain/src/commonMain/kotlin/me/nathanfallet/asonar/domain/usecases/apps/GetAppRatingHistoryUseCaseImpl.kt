package me.nathanfallet.asonar.domain.usecases.apps

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.snapshots.AppRatingHistory
import me.nathanfallet.asonar.domain.models.snapshots.AppRatingSnapshot
import me.nathanfallet.asonar.domain.repositories.AppRatingSnapshotsRepository
import me.nathanfallet.asonar.domain.usecases.apps.GetAppRatingHistoryUseCaseImpl.Companion.MIN_SPAN_DAYS

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
            ratingsPerDay = ratingsPerDay(snapshots),
            snapshots = snapshots,
        )
    }

    /**
     * Least-squares slope of ratingCount over time, in ratings per day. Null with under 2 readings,
     * or when they span less than [MIN_SPAN_DAYS] — extrapolating a daily rate (let alone a monthly
     * one) from a few hours of data gives a wild, meaningless number, so we hold it back until the
     * snapshots cover enough time. It fills in on its own as the history accrues.
     */
    private fun ratingsPerDay(snapshots: List<AppRatingSnapshot>): Double? {
        val points = snapshots.mapNotNull { snapshot -> snapshot.ratingCount?.let { it to snapshot.capturedAt } }
        if (points.size < 2) return null
        val origin = points.minOf { it.second }
        val xs = points.map { (it.second - origin).inWholeMilliseconds / 86_400_000.0 } // days
        if (xs.max() - xs.min() < MIN_SPAN_DAYS) return null
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

    companion object {
        /** Minimum span the readings must cover before we trust an extrapolated velocity. */
        private const val MIN_SPAN_DAYS = 1.0
    }

}
