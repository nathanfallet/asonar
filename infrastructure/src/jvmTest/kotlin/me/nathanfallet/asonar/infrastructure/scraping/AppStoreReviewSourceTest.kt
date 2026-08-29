package me.nathanfallet.asonar.infrastructure.scraping

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Live verification (hits Apple's real customer-reviews feed). Proves the plain-HTTP fetch and the
 * JSON navigation work, that the feed is genuinely newest-first — which the incremental walk depends
 * on — and that an app with no reviews in a market is an empty answer rather than a failure.
 */
class AppStoreReviewSourceTest {

    private val source = AppStoreReviewSource(HttpClient(CIO))

    /** Good Pizza, Great Pizza — a long-lived app with a full review feed in FR. */
    private val busyApp = "911121200"

    @Test
    fun fetchesAPageOfReviews() = runBlocking {
        val reviews = source.getReviews(busyApp, "FR", 1)
        println("[test] page 1 = ${reviews.size} reviews")
        assertTrue(reviews.isNotEmpty(), "the FR feed of $busyApp should return reviews")

        val first = reviews.first()
        println("[test] first = ${first.rating}★ | ${first.title} | ${first.content.take(60)}")
        assertTrue(first.externalId.isNotBlank(), "an id is required — it is the dedup key")
        assertTrue(first.content.isNotBlank(), "the review body is what discovery will mine")
        assertTrue(reviews.all { it.externalId.isNotBlank() })
        assertEquals(reviews.size, reviews.map { it.externalId }.distinct().size, "ids are unique in a page")
    }

    @Test
    fun ordersNewestFirst() = runBlocking {
        // The whole incremental walk rests on this: if the feed were not sorted by recency, stopping
        // at the first known review would silently skip everything after it.
        val reviews = source.getReviews(busyApp, "FR", 1)
        val dates = reviews.map { it.postedAt }
        assertEquals(dates.sortedDescending(), dates, "sortBy=mostRecent must really be newest-first")
    }

    @Test
    fun pagesAreDistinct() = runBlocking {
        val page1 = source.getReviews(busyApp, "FR", 1).map { it.externalId }.toSet()
        val page2 = source.getReviews(busyApp, "FR", 2).map { it.externalId }.toSet()
        assertTrue(page2.isNotEmpty(), "page 2 should exist for a busy app")
        assertTrue(page1.intersect(page2).isEmpty(), "paging must not repeat reviews")
    }

    @Test
    fun beyondTheLastPage_isEmptyNotAnError() = runBlocking {
        // Apple answers HTTP 400 past page 10; that must surface as "nothing more", not as a crash.
        assertEquals(emptyList(), source.getReviews(busyApp, "FR", 11))
    }
}
