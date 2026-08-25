package me.nathanfallet.asonar.domain.usecases.apps

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.snapshots.AppRatingHistory
import me.nathanfallet.asonar.domain.models.snapshots.AppRatingSnapshot
import me.nathanfallet.asonar.domain.repositories.AppRatingSnapshotsRepository

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

    /** Least-squares slope of ratingCount over time, in ratings per day. Null with under 2 readings. */
    private fun ratingsPerDay(snapshots: List<AppRatingSnapshot>): Double? {
        val points = snapshots.mapNotNull { snapshot -> snapshot.ratingCount?.let { it to snapshot.capturedAt } }
        if (points.size < 2) return null
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

}
