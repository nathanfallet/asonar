package me.nathanfallet.asonar.presentation.routes.keywords

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import me.nathanfallet.asonar.domain.models.keywords.CandidateStatus
import me.nathanfallet.asonar.presentation.routes.routesUnderTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Discovery is not in the client yet, so these go through plain HTTP. */
class KeywordCandidatesRoutesTest {

    private val dependencies = KeywordCandidatesRoutesDependencies(
        discoverKeywordCandidatesUseCase = mockk(),
        listKeywordCandidatesUseCase = mockk(),
        reviewKeywordCandidatesUseCase = mockk(),
    )

    private fun ApplicationTestBuilder.serve() = routesUnderTest { keywordCandidatesRoutes(dependencies) }

    @Test
    fun `the status filter defaults to the pending candidates`() = testApplication {
        serve()
        coEvery { dependencies.listKeywordCandidatesUseCase(1, any(), any()) } returns emptyList()

        val response = client.get("/api/keyword-candidates?appId=1")

        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { dependencies.listKeywordCandidatesUseCase(1, setOf(CandidateStatus.NEW), null) }
    }

    @Test
    fun `an unknown status is a 400`() = testApplication {
        serve()

        val response = client.get("/api/keyword-candidates?appId=1&status=NEW,MAYBE")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        coVerify(exactly = 0) { dependencies.listKeywordCandidatesUseCase(any(), any(), any()) }
    }

    @Test
    fun `candidates of an unknown app are a 404`() = testApplication {
        serve()
        coEvery { dependencies.listKeywordCandidatesUseCase(2, any(), any()) } returns null

        assertEquals(HttpStatusCode.NotFound, client.get("/api/keyword-candidates?appId=2").status)
    }

}
