package me.nathanfallet.asonar.domain.usecases.reviews

import kotlinx.coroutines.runBlocking
import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.apps.App
import me.nathanfallet.asonar.domain.models.apps.AppPayload
import me.nathanfallet.asonar.domain.models.apps.AppRole
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.keywords.Keyword
import me.nathanfallet.asonar.domain.models.keywords.KeywordPayload
import me.nathanfallet.asonar.domain.models.reviews.AppReview
import me.nathanfallet.asonar.domain.models.reviews.AppReviewPayload
import me.nathanfallet.asonar.domain.models.reviews.FetchedReview
import me.nathanfallet.asonar.domain.repositories.AppReviewsRepository
import me.nathanfallet.asonar.domain.repositories.AppsRepository
import me.nathanfallet.asonar.domain.repositories.KeywordsRepository
import me.nathanfallet.asonar.domain.services.AppReviewSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * Locks the two rules that make review collection cheap to re-run: stop at the first review we
 * already hold, and write everything in ONE batch.
 */
class FetchAppReviewsTest {

    private val now = Clock.System.now()
    private val app = App(1, Store.APP_STORE, "6779967120", "NutriMaxing", AppRole.OWNED, now)

    @Test
    fun stopsAtTheFirstKnownReview_andDoesNotWalkFurther() = runBlocking {
        // Page 1 holds two new reviews then one we already have; page 2 must never be requested.
        val source = FakeSource(
            pages = mapOf(
                1 to listOf(review("r5"), review("r4"), review("r3")),
                2 to listOf(review("r2"), review("r1")),
            )
        )
        val reviews = FakeReviews(known = setOf("r3", "r2", "r1"))
        val useCase = useCase(source, reviews, tracked = listOf(keyword("FR")))

        val recorded = useCase(app.id, listOf("FR"))

        assertEquals(listOf("r5", "r4"), recorded?.map { it.externalId })
        assertEquals(listOf(1), source.pagesRequested, "the walk stops on the page that hit the frontier")
    }

    @Test
    fun firstRun_walksEveryPageUntilTheFeedRunsOut() = runBlocking {
        val source = FakeSource(
            pages = mapOf(
                1 to listOf(review("r4"), review("r3")),
                2 to listOf(review("r2"), review("r1")),
                3 to emptyList(),
            )
        )
        val reviews = FakeReviews(known = emptySet())
        val useCase = useCase(source, reviews, tracked = listOf(keyword("FR")))

        val recorded = useCase(app.id, listOf("FR"))

        assertEquals(listOf("r4", "r3", "r2", "r1"), recorded?.map { it.externalId })
        assertEquals(listOf(1, 2, 3), source.pagesRequested, "an empty page ends the feed")
    }

    @Test
    fun everythingIsWrittenInASingleBatch() = runBlocking {
        val source = FakeSource(
            pages = mapOf(1 to listOf(review("fr1")), 2 to emptyList()),
            perCountry = mapOf("US" to mapOf(1 to listOf(review("us1")), 2 to emptyList())),
        )
        val reviews = FakeReviews(known = emptySet())
        val useCase = useCase(source, reviews, tracked = listOf(keyword("FR"), keyword("US")))

        useCase(app.id, listOf("FR", "US"))

        assertEquals(1, reviews.writes.size, "one batch insert for the whole run, markets included")
        assertEquals(listOf("fr1", "us1"), reviews.writes.single().map { it.externalId })
    }

    @Test
    fun defaultMarkets_areTheCountriesWeTrackKeywordsIn() = runBlocking {
        val source = FakeSource(pages = mapOf(1 to emptyList()))
        val useCase = useCase(
            source,
            FakeReviews(known = emptySet()),
            // DE appears twice and belongs to another store: neither should widen the perimeter.
            tracked = listOf(keyword("FR"), keyword("US"), keyword("FR"), keyword("DE", Store.PLAY_STORE)),
        )

        useCase(app.id, null)

        assertEquals(listOf("FR", "US"), source.countriesRequested)
        assertTrue("DE" !in source.countriesRequested, "a market of another store is not ours to fetch")
    }

    @Test
    fun anUnknownApp_yieldsNull() = runBlocking {
        val useCase = useCase(FakeSource(emptyMap()), FakeReviews(emptySet()), emptyList())
        assertEquals(null, useCase(404, null))
    }

    // --- helpers ---

    private fun review(id: String) = FetchedReview(externalId = id, content = "content $id", postedAt = now)

    private fun keyword(country: String, store: Store = Store.APP_STORE) =
        Keyword(1, "nutrition", store, country, now)

    private fun useCase(source: AppReviewSource, reviews: FakeReviews, tracked: List<Keyword>) =
        FetchAppReviewsUseCaseImpl(
            appsRepository = FakeApps(app),
            keywordsRepository = FakeKeywords(tracked),
            appReviewsRepository = reviews,
            appReviewSources = listOf(source),
        )

    private class FakeSource(
        private val pages: Map<Int, List<FetchedReview>>,
        private val perCountry: Map<String, Map<Int, List<FetchedReview>>> = emptyMap(),
    ) : AppReviewSource {
        override val store = Store.APP_STORE
        override val maxPages = 10
        var pagesRequested: List<Int> = emptyList()
            private set
        var countriesRequested: List<String> = emptyList()
            private set

        override suspend fun getReviews(storeAppId: String, country: String, page: Int): List<FetchedReview> {
            pagesRequested = pagesRequested + page
            if (country !in countriesRequested) countriesRequested = countriesRequested + country
            return (perCountry[country] ?: pages)[page].orEmpty()
        }
    }

    private class FakeReviews(private val known: Set<String>) : AppReviewsRepository {
        var writes: List<List<AppReviewPayload>> = emptyList()
            private set

        override suspend fun list(store: Store, storeAppId: String, country: String, pagination: Pagination) =
            emptyList<AppReview>()

        override suspend fun knownExternalIds(
            store: Store,
            storeAppId: String,
            country: String,
            externalIds: List<String>,
        ) = externalIds.filter { it in known }.toSet()

        override suspend fun createAll(payloads: List<AppReviewPayload>): List<AppReview> {
            writes = writes + listOf(payloads)
            return payloads.mapIndexed { index, p ->
                AppReview(
                    index.toLong(), p.store, p.storeAppId, p.country, p.externalId,
                    p.author, p.title, p.content, p.rating, p.version, p.postedAt, p.fetchedAt,
                )
            }
        }
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
        override suspend fun get(id: Long) = keywords.firstOrNull()
        override suspend fun getByTerm(term: String, store: Store, country: String): Keyword? = null
        override suspend fun create(payload: KeywordPayload) = error("not used")
        override suspend fun delete(id: Long) = false
    }
}
