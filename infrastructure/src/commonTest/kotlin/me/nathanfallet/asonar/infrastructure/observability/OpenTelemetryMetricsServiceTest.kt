package me.nathanfallet.asonar.infrastructure.observability

import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import me.nathanfallet.asonar.domain.models.apps.Store
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class OpenTelemetryMetricsServiceTest {

    private val reader = InMemoryMetricReader.create()
    private val meter = SdkMeterProvider.builder().registerMetricReader(reader).build().get("asonar")
    private val service = OpenTelemetryMetricsService(mockk { every { getMeter() } returns meter })

    private fun metric(name: String): MetricData = reader.collectAllMetrics().single { it.name == name }

    @Test
    fun `a keyword fetch counts once and records its duration, with what it refreshed`() {
        service.recordKeywordFetch(Store.APP_STORE, refreshedRanking = true, refreshedPopularity = false, 1500.milliseconds)

        val metrics = reader.collectAllMetrics().associateBy { it.name }
        val count = metrics.getValue("asonar.keyword.fetches").longSumData.points.single()
        assertEquals(1, count.value)
        assertEquals("APP_STORE", count.attributes.get(AttributeKey.stringKey("asonar.store")))
        assertEquals(true, count.attributes.get(AttributeKey.booleanKey("asonar.refreshed.ranking")))
        assertEquals(false, count.attributes.get(AttributeKey.booleanKey("asonar.refreshed.popularity")))
        assertEquals(1.5, metrics.getValue("asonar.keyword.fetch.duration").histogramData.points.single().sum)
    }

    @Test
    fun `popularity reads are split by result`() {
        service.recordPopularityRead(Store.APP_STORE, found = true)
        service.recordPopularityRead(Store.APP_STORE, found = false)
        service.recordPopularityRead(Store.APP_STORE, found = false)

        val byResult: Map<String?, Long> = metric("asonar.popularity.reads").longSumData.points
            .associate { it.attributes.get(AttributeKey.stringKey("asonar.result")) to it.value }
        assertEquals(mapOf<String?, Long>("found" to 1L, "missing" to 2L), byResult)
    }

}
