package me.nathanfallet.asonar.presentation.routes.apps

import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import me.nathanfallet.asonar.api.requests.apps.RegisterAppRequest
import me.nathanfallet.asonar.api.responses.apps.AppResponse
import me.nathanfallet.asonar.domain.models.apps.App
import me.nathanfallet.asonar.domain.models.apps.AppPayload
import me.nathanfallet.asonar.domain.models.apps.AppRole
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.usecases.apps.DeleteAppUseCase
import me.nathanfallet.asonar.domain.usecases.apps.GetAppUseCase
import me.nathanfallet.asonar.domain.usecases.apps.GetOrCreateAppUseCase
import me.nathanfallet.asonar.domain.usecases.apps.ListAppsUseCase
import me.nathanfallet.asonar.presentation.routes.routesUnderTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class AppsRoutesTest {

    private val createdAt = Instant.parse("2026-10-01T12:00:00Z")
    private val app = App(1, Store.APP_STORE, "6779967120", "NutriMaxing", AppRole.OWNED, createdAt)
    private val response = AppResponse(1, "APP_STORE", "6779967120", "NutriMaxing", "OWNED", createdAt)

    private val listApps = mockk<ListAppsUseCase>()
    private val getApp = mockk<GetAppUseCase>()
    private val getOrCreateApp = mockk<GetOrCreateAppUseCase>()
    private val deleteApp = mockk<DeleteAppUseCase>()

    private fun ApplicationTestBuilder.client() = routesUnderTest {
        appsRoutes(AppsRoutesDependencies(listApps, getApp, getOrCreateApp, deleteApp))
    }

    @Test
    fun `lists the apps`() = testApplication {
        coEvery { listApps() } returns listOf(app)

        assertEquals(listOf(response), client().apps.getAll().apps)
    }

    @Test
    fun `reads one app`() = testApplication {
        coEvery { getApp(1) } returns app

        assertEquals(response, client().apps.get(1))
    }

    @Test
    fun `an unknown app is a 404`() = testApplication {
        coEvery { getApp(2) } returns null

        val error = assertFailsWith<ClientRequestException> { client().apps.get(2) }
        assertEquals(HttpStatusCode.NotFound, error.response.status)
    }

    @Test
    fun `registering parses the store and the role`() = testApplication {
        val competitor = app.copy(role = AppRole.COMPETITOR)
        coEvery {
            getOrCreateApp(AppPayload(Store.APP_STORE, "6779967120", "NutriMaxing", AppRole.COMPETITOR))
        } returns competitor

        val registered = client().apps.register(RegisterAppRequest("APP_STORE", "6779967120", "NutriMaxing", "COMPETITOR"))

        assertEquals("COMPETITOR", registered.role)
    }

    @Test
    fun `registering on an unknown store is a 400 and creates nothing`() = testApplication {
        val error = assertFailsWith<ClientRequestException> {
            client().apps.register(RegisterAppRequest("WINDOWS_STORE", "1", "X"))
        }

        assertEquals(HttpStatusCode.BadRequest, error.response.status)
        coVerify(exactly = 0) { getOrCreateApp(any()) }
    }

    @Test
    fun `deleting an unknown app is a 404`() = testApplication {
        coEvery { deleteApp(2) } returns false

        val error = assertFailsWith<ClientRequestException> { client().apps.delete(2) }
        assertEquals(HttpStatusCode.NotFound, error.response.status)
    }

}
