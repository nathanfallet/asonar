package me.nathanfallet.asonar

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Boots the real `module()` — every Koin binding, every plugin, every route — on an in-memory H2.
 * A missing binding or a broken config otherwise only shows when someone runs the app by hand.
 */
class ApplicationTest {

    @Test
    fun theAppBootsAndAnswersItsProbe() = testApplication {
        environment {
            config = ApplicationConfig("application.test.conf")
        }
        application {
            module()
        }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }

}
