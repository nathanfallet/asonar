package me.nathanfallet.asonar.infrastructure.config

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry
import me.nathanfallet.asonar.infrastructure.observability.TelemetryFactory
import org.koin.ktor.ext.inject

/**
 * Traces every HTTP call and exports Ktor's server metrics (request counts, latencies, JVM) through
 * OpenTelemetry. The traces continue through RabbitMQ into the fetch a call queues, and down to each
 * SQL statement.
 */
fun Application.configureTelemetry() {
    val telemetryFactory by inject<TelemetryFactory>()
    install(KtorServerTelemetry) {
        setOpenTelemetry(telemetryFactory.getOpenTelemetry())
    }
    install(MicrometerMetrics) {
        registry = OpenTelemetryMeterRegistry.create(telemetryFactory.getOpenTelemetry())
    }
}
