package me.nathanfallet.asonar.infrastructure.observability

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.services.MetricsCollectorService
import kotlin.time.Duration
import kotlin.time.DurationUnit

/** [MetricsCollectorService] that records to OpenTelemetry. */
class OpenTelemetryMetricsService(
    telemetryFactory: TelemetryFactory,
) : MetricsCollectorService {

    private val meter = telemetryFactory.getMeter()

    private val keywordFetches = meter.counterBuilder("asonar.keyword.fetches")
        .setDescription("Keyword fetches, by what they refreshed (neither: the data was still fresh)")
        .setUnit("{fetch}")
        .build()

    private val keywordFetchDuration = meter.histogramBuilder("asonar.keyword.fetch.duration")
        .setDescription("How long one keyword fetch took, store calls included")
        .setUnit("s")
        .build()

    private val popularityReads = meter.counterBuilder("asonar.popularity.reads")
        .setDescription("Popularity reads, by result (missing: usually an expired Apple Search Ads session)")
        .setUnit("{read}")
        .build()

    override fun recordKeywordFetch(
        store: Store,
        refreshedRanking: Boolean,
        refreshedPopularity: Boolean,
        duration: Duration,
    ) {
        val attributes = Attributes.of(
            STORE, store.name,
            REFRESHED_RANKING, refreshedRanking,
            REFRESHED_POPULARITY, refreshedPopularity,
        )
        keywordFetches.add(1, attributes)
        keywordFetchDuration.record(duration.toDouble(DurationUnit.SECONDS), attributes)
    }

    override fun recordPopularityRead(store: Store, found: Boolean) {
        popularityReads.add(1, Attributes.of(STORE, store.name, RESULT, if (found) "found" else "missing"))
    }

    private companion object {
        val STORE: AttributeKey<String> = AttributeKey.stringKey("asonar.store")
        val REFRESHED_RANKING: AttributeKey<Boolean> = AttributeKey.booleanKey("asonar.refreshed.ranking")
        val REFRESHED_POPULARITY: AttributeKey<Boolean> = AttributeKey.booleanKey("asonar.refreshed.popularity")
        val RESULT: AttributeKey<String> = AttributeKey.stringKey("asonar.result")
    }

}
