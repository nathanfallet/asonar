package me.nathanfallet.asonar.domain.usecases.keywords

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.keywords.Keyword
import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshot
import me.nathanfallet.asonar.domain.models.snapshots.TopAppSnapshot
import me.nathanfallet.asonar.domain.repositories.KeywordsRepository
import me.nathanfallet.asonar.domain.repositories.PopularitySnapshotsRepository
import me.nathanfallet.asonar.domain.repositories.TopAppSnapshotsRepository
import me.nathanfallet.asonar.domain.services.KeywordPopularitySource
import me.nathanfallet.asonar.domain.services.MetricsCollectorService
import kotlin.test.Test
import kotlin.time.Clock

/** What a keyword fetch reports, which is what the dashboards and alerts read. */
class FetchKeywordMetricsTest {

    private val now = Clock.System.now()
    private val keyword = Keyword(7, "nutrition", Store.APP_STORE, "FR", now)

    private val keywords = mockk<KeywordsRepository> { coEvery { get(7) } returns keyword }
    private val topApps = mockk<TopAppSnapshotsRepository>()
    private val popularitySnapshots = mockk<PopularitySnapshotsRepository>()
    private val popularitySource = mockk<KeywordPopularitySource> { every { store } returns Store.APP_STORE }
    private val metrics = mockk<MetricsCollectorService>(relaxed = true)

    private val useCase = FetchKeywordUseCaseImpl(
        keywordsRepository = keywords,
        appsRepository = mockk(relaxed = true),
        popularitySources = listOf(popularitySource),
        appSearchSources = emptyList(),
        appSubtitleSources = emptyList(),
        opportunityScorers = emptyList(),
        recordKeywordRunUseCase = mockk(relaxed = true),
        getAppRatingHistoryUseCase = mockk(relaxed = true),
        keywordSignalsRepository = mockk(relaxed = true),
        popularitySnapshotsRepository = popularitySnapshots,
        topAppSnapshotsRepository = topApps,
        metricsCollectorService = metrics,
    )

    private fun freshRanking() =
        coEvery { topApps.listLatestForKeyword(7) } returns listOf(TopAppSnapshot(1, 7, 1, "1", "App", capturedAt = now))

    @Test
    fun `fresh data is reported as a fetch that refreshed nothing`() = runBlocking {
        freshRanking()
        coEvery { popularitySnapshots.getLatestForKeyword(7) } returns PopularitySnapshot(1, 7, 42, now)

        useCase(7)

        verify(exactly = 1) { metrics.recordKeywordFetch(Store.APP_STORE, false, false, any()) }
        verify(exactly = 0) { metrics.recordPopularityRead(any(), any()) }
    }

    @Test
    fun `a popularity the source could not read is reported missing`() = runBlocking {
        freshRanking()
        coEvery { popularitySnapshots.getLatestForKeyword(7) } returns null
        coEvery { popularitySource.getPopularity("nutrition", "FR") } returns null

        useCase(7)

        verify(exactly = 1) { metrics.recordPopularityRead(Store.APP_STORE, found = false) }
        verify(exactly = 1) { metrics.recordKeywordFetch(Store.APP_STORE, false, true, any()) }
    }

    @Test
    fun `a popularity read is reported found`() = runBlocking {
        freshRanking()
        coEvery { popularitySnapshots.getLatestForKeyword(7) } returns null
        coEvery { popularitySource.getPopularity("nutrition", "FR") } returns 42

        useCase(7)

        verify(exactly = 1) { metrics.recordPopularityRead(Store.APP_STORE, found = true) }
    }

}
