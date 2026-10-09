package me.nathanfallet.asonar.infrastructure.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import io.opentelemetry.semconv.ServiceAttributes

/**
 * [TelemetryFactory] over the autoconfigured SDK: everything is set by the standard `OTEL_*`
 * environment variables — `OTEL_EXPORTER_OTLP_ENDPOINT` defaults to `localhost:4317`, where a local
 * `grafana/otel-lgtm` listens, and `OTEL_SDK_DISABLED=true` turns it all off.
 *
 * [enabled] is false under test, where nothing should try to export.
 */
class TelemetryFactoryImpl(
    private val enabled: Boolean,
    private val environment: String,
) : TelemetryFactory {

    private val sdk: OpenTelemetry by lazy {
        if (!enabled) return@lazy OpenTelemetry.noop()
        AutoConfiguredOpenTelemetrySdk.builder()
            .addResourceCustomizer { resource, _ ->
                resource.toBuilder()
                    .put(ServiceAttributes.SERVICE_NAME, SERVICE_NAME)
                    .put(DEPLOYMENT_ENVIRONMENT, environment)
                    .build()
            }
            // Also the global instance, for libraries that only look there.
            .setResultAsGlobal()
            .build()
            .openTelemetrySdk
    }

    override fun getOpenTelemetry(): OpenTelemetry = sdk

    override fun getTracer(): Tracer = sdk.getTracer(SERVICE_NAME)

    override fun getMeter(): Meter = sdk.getMeter(SERVICE_NAME)

    private companion object {
        const val SERVICE_NAME = "asonar"

        // Still incubating in semconv, hence spelled out rather than imported.
        const val DEPLOYMENT_ENVIRONMENT = "deployment.environment.name"
    }

}
