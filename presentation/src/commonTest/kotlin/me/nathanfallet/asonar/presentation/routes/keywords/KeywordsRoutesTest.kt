package me.nathanfallet.asonar.presentation.routes.keywords

import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import me.nathanfallet.asonar.api.requests.keywords.TrackKeywordRequest
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.keywords.Keyword
import me.nathanfallet.asonar.domain.models.keywords.KeywordDetail
import me.nathanfallet.asonar.domain.models.keywords.KeywordOverview
import me.nathanfallet.asonar.domain.models.keywords.KeywordPayload
import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshot
import me.nathanfallet.asonar.presentation.routes.routesUnderTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class KeywordsRoutesTest {

    private val createdAt = Instant.parse("2026-10-01T12:00:00Z")
    private val keyword = Keyword(7, "nutrition", Store.APP_STORE, "FR", createdAt)
    private val popularity = PopularitySnapshot(1, 7, 42, Instant.parse("2026-10-02T08:00:00Z"))

    private val dependencies = KeywordsRoutesDependencies(
        listKeywordOverviewsUseCase = mockk(),
        getKeywordDetailUseCase = mockk(),
        getOrCreateKeywordUseCase = mockk(),
        deleteKeywordUseCase = mockk(),
        listPopularityHistoryUseCase = mockk(),
        getLatestTopAppsUseCase = mockk(),
        listRankHistoryUseCase = mockk(),
        refreshKeywordUseCase = mockk(),
    )

    private fun ApplicationTestBuilder.client() = routesUnderTest { keywordsRoutes(dependencies) }

    @Test
    fun `lists every keyword with its latest popularity`() = testApplication {
        coEvery { dependencies.listKeywordOverviewsUseCase(any()) } returns listOf(KeywordOverview(keyword, popularity))

        val listed = client().keywords.getAll().keywords.single()

        assertEquals("nutrition", listed.term)
        assertEquals(42, listed.latestPopularity)
        assertEquals(popularity.capturedAt, listed.latestPopularityAt)
    }

    @Test
    fun `tracking parses the store`() = testApplication {
        coEvery { dependencies.getOrCreateKeywordUseCase(KeywordPayload("nutrition", Store.APP_STORE, "FR")) } returns keyword

        val tracked = client().keywords.track(TrackKeywordRequest("nutrition", "APP_STORE", "FR"))

        assertEquals(7, tracked.id)
    }

    @Test
    fun `tracking on an unknown store is a 400 and tracks nothing`() = testApplication {
        val error = assertFailsWith<ClientRequestException> {
            client().keywords.track(TrackKeywordRequest("nutrition", "WINDOWS_STORE", "FR"))
        }

        assertEquals(HttpStatusCode.BadRequest, error.response.status)
        coVerify(exactly = 0) { dependencies.getOrCreateKeywordUseCase(any()) }
    }

    @Test
    fun `reads a keyword's detail`() = testApplication {
        coEvery { dependencies.getKeywordDetailUseCase(7) } returns KeywordDetail(keyword, popularity)

        val detail = client().keywords.get(7)

        assertEquals(42, detail.keyword.latestPopularity)
    }

    @Test
    fun `an unknown keyword is a 404`() = testApplication {
        coEvery { dependencies.getKeywordDetailUseCase(8) } returns null

        val error = assertFailsWith<ClientRequestException> { client().keywords.get(8) }
        assertEquals(HttpStatusCode.NotFound, error.response.status)
    }

    @Test
    fun `refreshing queues a fetch`() = testApplication {
        coEvery { dependencies.refreshKeywordUseCase(7) } returns true

        // The refresh route is declared by path, not by typed resource: this pins that the client's
        // KeywordsApi.Id.Refresh URL still reaches it.
        client().keywords.refresh(7)

        coVerify(exactly = 1) { dependencies.refreshKeywordUseCase(7) }
    }

    @Test
    fun `refreshing an unknown keyword is a 404`() = testApplication {
        coEvery { dependencies.refreshKeywordUseCase(8) } returns false

        val error = assertFailsWith<ClientRequestException> { client().keywords.refresh(8) }
        assertEquals(HttpStatusCode.NotFound, error.response.status)
    }

}
