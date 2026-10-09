package me.nathanfallet.asonar.presentation.config

import io.ktor.callid.KtorCallIdContextElement
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.currentCoroutineContext
import org.slf4j.MDC
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MonitoringTest {

    /** Answers "<MDC id>|<coroutine-context id>", as seen from inside a route. */
    private fun ApplicationTestBuilder.serve() = application {
        configureMonitoring()
        routing {
            get("/probe") {
                val context = currentCoroutineContext()[KtorCallIdContextElement]?.callId
                call.respondText("${MDC.get("call-id")}|$context")
            }
        }
    }

    @Test
    fun `the caller's request id is kept, echoed, and visible to the route`() = testApplication {
        serve()

        val response = client.get("/probe") { header(HttpHeaders.XRequestId, "req-1") }

        assertEquals("req-1", response.headers[HttpHeaders.XRequestId])
        // The MDC feeds the logs; the coroutine context feeds the RabbitMQ headers.
        assertEquals("req-1|req-1", response.bodyAsText())
    }

    @Test
    fun `a request without an id gets a fresh one`() = testApplication {
        serve()

        val response = client.get("/probe")

        val id = response.headers[HttpHeaders.XRequestId]
        assertTrue(!id.isNullOrBlank())
        assertEquals("$id|$id", response.bodyAsText())
    }

    @Test
    fun `an unprintable caller id is replaced, never logged as is`() = testApplication {
        serve()

        val response = client.get("/probe") { header(HttpHeaders.XRequestId, "bad id\twith\tspaces") }

        assertNotEquals("bad id\twith\tspaces", response.headers[HttpHeaders.XRequestId])
    }

}
