package me.nathanfallet.asonar.domain.usecases.keywords

import kotlinx.coroutines.runBlocking
import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.apps.App
import me.nathanfallet.asonar.domain.models.apps.AppPayload
import me.nathanfallet.asonar.domain.models.apps.AppRole
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.keywords.*
import me.nathanfallet.asonar.domain.models.search.KeywordSuggestion
import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshot
import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshotPayload
import me.nathanfallet.asonar.domain.repositories.*
import me.nathanfallet.asonar.domain.services.KeywordSuggestionSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * Locks what a discovery pass proposes. The rules that matter here are the ones that keep the review
 * list worth reading: never propose what we already track, and seed from the terms that actually have
 * volume (a floored seed's neighbourhood is floored too).
 */
class DiscoverKeywordCandidatesTest {

    private val now = Clock.System.now()
    private val app = App(1, Store.APP_STORE, "6779967120", "NutriMaxing", AppRole.OWNED, now)

    @Test
    fun proposals_excludeTermsWeAlreadyTrack() = runBlocking {
        val source = FakeSource(listOf("nutrition tracker", "food tracker", "nutrition"))
        val repository = FakeCandidates()
        val useCase = useCase(
            tracked = listOf(keyword(1, "nutrition", "FR"), keyword(2, "food tracker", "FR")),
            candidates = repository,
            sources = listOf(source),
        )

        useCase(app.id, listOf("FR"), listOf("nutrition"))

        assertEquals(
            listOf("nutrition tracker"),
            repository.received.map { it.term },
            "a term already tracked in the market is not a candidate",
        )
    }

    @Test
    fun defaultSeeds_takeTheBestMeasuredTermsOfTheMarket_capped() = runBlocking {
        // Ten tracked terms with descending popularity, plus one from another market that must not leak.
        val tracked = (1..10).map { keyword(it.toLong(), "term$it", "FR") } +
                keyword(99, "term-us", "US")
        val popularity = (1..10).associate { it.toLong() to snapshot(it.toLong(), 100 - it) }
        val source = FakeSource(emptyList())
        val useCase = useCase(
            tracked = tracked,
            candidates = FakeCandidates(),
            sources = listOf(source),
            popularity = popularity,
        )

        useCase(app.id, listOf("FR"), null)

        assertEquals(
            DiscoverKeywordCandidatesUseCaseImpl.MAX_SEEDS_PER_MARKET,
            source.seedsUsed.size,
            "seeds are capped — each one costs a source request",
        )
        assertEquals(
            listOf("term1", "term2", "term3", "term4", "term5", "term6", "term7", "term8"),
            source.seedsUsed,
            "best-measured first",
        )
        assertTrue("term-us" !in source.seedsUsed, "another market's terms are not seeds here")
    }

    @Test
    fun aStoreWithoutSources_proposesNothing() = runBlocking {
        val repository = FakeCandidates()
        val useCase = useCase(
            tracked = listOf(keyword(1, "nutrition", "FR")),
            candidates = repository,
            // A Play Store source can't answer for an App Store app.
            sources = listOf(FakeSource(listOf("whatever"), store = Store.PLAY_STORE)),
        )

        val result = useCase(app.id, listOf("FR"), listOf("nutrition"))

        assertEquals(emptyList(), result?.created)
        assertTrue(repository.received.isEmpty(), "nothing is written when no source covers the store")
    }

    @Test
    fun anUnknownApp_isNotDiscoverable() = runBlocking {
        val useCase = useCase(tracked = emptyList(), candidates = FakeCandidates(), sources = emptyList())
        assertEquals(null, useCase(404, null, null))
    }

    // --- helpers ---

    private fun keyword(id: Long, term: String, country: String) =
        Keyword(id, term, Store.APP_STORE, country, now)

    private fun snapshot(keywordId: Long, popularity: Int) =
        PopularitySnapshot(keywordId, keywordId, popularity, now)

    private fun useCase(
        tracked: List<Keyword>,
        candidates: FakeCandidates,
        sources: List<KeywordSuggestionSource>,
        popularity: Map<Long, PopularitySnapshot> = emptyMap(),
    ) = DiscoverKeywordCandidatesUseCaseImpl(
        appsRepository = FakeApps(app),
        keywordsRepository = FakeKeywords(tracked),
        keywordCandidatesRepository = candidates,
        popularitySnapshotsRepository = FakePopularity(popularity),
        suggestionSources = sources,
    )

    private class FakeSource(
        private val terms: List<String>,
        override val store: Store = Store.APP_STORE,
    ) : KeywordSuggestionSource {
        override val source = CandidateSource.ASA
        var seedsUsed: List<String> = emptyList()
            private set

        override suspend fun suggest(
            storeAppId: String,
            country: String,
            seeds: List<String>,
        ): List<KeywordSuggestion> {
            seedsUsed = seeds
            return terms.map { KeywordSuggestion(it, popularity = 20) }
        }
    }

    private class FakeCandidates : KeywordCandidatesRepository {
        var received: List<KeywordCandidatePayload> = emptyList()
            private set

        override suspend fun list(appId: Long, statuses: Set<CandidateStatus>) = emptyList<KeywordCandidate>()
        override suspend fun get(id: Long): KeywordCandidate? = null
        override suspend fun upsertAll(payloads: List<KeywordCandidatePayload>): KeywordCandidateUpsert {
            received = payloads
            return KeywordCandidateUpsert(emptyList(), emptyList())
        }

        override suspend fun updateStatus(ids: List<Long>, status: CandidateStatus) = 0
        override suspend fun deleteForApp(appId: Long) = 0
    }

    private class FakeApps(private val app: App) : AppsRepository {
        override suspend fun list() = listOf(app)
        override suspend fun get(id: Long) = app.takeIf { it.id == id }
        override suspend fun getByStoreAppId(store: Store, storeAppId: String): App? = null
        override suspend fun create(payload: AppPayload) = app
        override suspend fun updateRole(id: Long, role: AppRole): App? = null
        override suspend fun delete(id: Long) = false
    }

    private class FakeKeywords(private val keywords: List<Keyword>) : KeywordsRepository {
        override suspend fun list(pagination: Pagination) = keywords
        override suspend fun get(id: Long) = keywords.firstOrNull { it.id == id }
        override suspend fun getByTerm(term: String, store: Store, country: String): Keyword? = null
        override suspend fun create(payload: KeywordPayload) = error("not used")
        override suspend fun delete(id: Long) = false
    }

    private class FakePopularity(
        private val latest: Map<Long, PopularitySnapshot>,
    ) : PopularitySnapshotsRepository {
        override suspend fun listForKeyword(keywordId: Long, pagination: Pagination) = emptyList<PopularitySnapshot>()
        override suspend fun getLatestForKeyword(keywordId: Long) = latest[keywordId]
        override suspend fun latestByKeyword() = latest
        override suspend fun create(payload: PopularitySnapshotPayload) = error("not used")
    }
}
