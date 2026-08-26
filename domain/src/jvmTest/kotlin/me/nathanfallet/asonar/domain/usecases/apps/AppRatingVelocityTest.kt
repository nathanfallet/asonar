package me.nathanfallet.asonar.domain.usecases.apps

import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.snapshots.AppRatingSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

/**
 * Locks the review-velocity semantics used by the opportunity scoring:
 *  - the 30-day figure is the ratings actually gained, **never extrapolated past the window we've
 *    observed** — a young app doesn't read a made-up momentum from a couple of days of data;
 *  - it needs readings on **at least two distinct calendar days** — points clustered within one day
 *    aren't a representative trend.
 */
class AppRatingVelocityTest {

    private val hourMs = 3_600_000L

    // A fixed midday origin, so hour offsets within a day stay on the same calendar day.
    private val originMs = 100L * 24 * hourMs + 12 * hourMs

    /** A reading `hoursAgo` before the fixed midday origin (no clock needed — only spans matter). */
    private fun reading(hoursAgo: Long, count: Int?) = AppRatingSnapshot(
        id = 0,
        store = Store.APP_STORE,
        storeAppId = "1",
        country = "US",
        name = "App",
        ratingCount = count,
        averageRating = null,
        capturedAt = Instant.fromEpochMilliseconds(originMs - hoursAgo * hourMs),
    )

    private fun gained(vararg readings: AppRatingSnapshot) =
        GetAppRatingHistoryUseCaseImpl.ratingsGained(readings.toList())

    // Nathan's case: first seen 10 days ago at 0, now at 10 → +10 over 30d, NOT +30. We must not
    // invent a negative count 30 days ago (which is what slope × 30 would imply: 10 − (−20)).
    @Test
    fun zeroToTen_overTenDays_isPlusTenNotThirty() {
        assertEquals(10, gained(reading(10 * 24, 0), reading(0, 10)))
    }

    // Even a short (but multi-day) window mustn't overshoot: 0 → 10 across 2 days is +10, not +150.
    @Test
    fun zeroToTen_overTwoDays_isPlusTenNotProjection() {
        assertEquals(10, gained(reading(2 * 24, 0), reading(0, 10)))
    }

    // Once the history covers 30 days or more it IS the real monthly rate: 0 → 40 over 40 days → +30.
    @Test
    fun steadyGain_overFortyDays_isRealMonthlyRate() {
        assertEquals(30, gained(reading(40 * 24, 0), reading(0, 40)))
    }

    // A single reading can't yield any velocity.
    @Test
    fun singleReading_isNull() {
        assertNull(gained(reading(0, 5)))
    }

    // Three readings a few hours apart on the SAME calendar day → not representative → null.
    @Test
    fun sameDayCluster_isNull() {
        assertNull(gained(reading(6, 0), reading(3, 5), reading(0, 10)))
    }

    // Two calendar days is exactly enough (yesterday and today) → velocity computes.
    @Test
    fun twoDistinctDays_computes() {
        assertEquals(10, gained(reading(24, 0), reading(0, 10)))
    }
}
