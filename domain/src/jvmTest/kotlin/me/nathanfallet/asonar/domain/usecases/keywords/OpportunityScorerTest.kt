package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.keywords.CompetitorSignal
import me.nathanfallet.asonar.domain.models.keywords.OpportunityVerdict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpportunityScorerTest {

    // position, title factor (1 title / .5 subtitle / 0 none), total ratings, velocity (ratings/30d)
    private fun c(pos: Int, title: Double, reviews: Int? = null, vel: Int? = null) =
        CompetitorSignal(pos, title, reviews, vel)

    private fun run(
        pop: Int? = 50,
        competitors: List<CompetitorSignal>,
        ourVel: Int? = null,
        total: Int? = 1000,
        label: String = "",
    ): OpportunityScorer.Result {
        val r = OpportunityScorer.score(OpportunityScorer.Inputs(pop, competitors, ourVel, total))
        println("[scorer] ${label.padEnd(34)} verdict=${r.verdict} score=${r.score} wall=${(r.wallStrength * 100).toInt()}%")
        return r
    }

    // The #1 holder is huge on reviews but doesn't use the term → it's there "by accident", easy to
    // pass; the term-users sit lower with few reviews. Should be a YES.
    @Test
    fun weakTopHolder_bigReviewsButNoTitle_isYes() {
        val r = run(
            pop = 55,
            competitors = listOf(c(1, 0.0, reviews = 80_000), c(2, 1.0, reviews = 300), c(3, 1.0, reviews = 200)),
            label = "weak #1 (no title, huge reviews)",
        )
        assertEquals(OpportunityVerdict.YES, r.verdict)
    }

    // #1 uses the term but has almost no reviews (weakly anchored); a huge app sits lower WITHOUT the
    // term. Should be a YES — the actual top-of-the-term is beatable.
    @Test
    fun topUsesTermButFewReviews_isYes() {
        val r = run(
            pop = 55,
            competitors = listOf(c(1, 1.0, reviews = 15), c(2, 1.0, reviews = 40), c(3, 0.0, reviews = 90_000)),
            label = "#1 uses term, tiny reviews",
        )
        assertEquals(OpportunityVerdict.YES, r.verdict)
    }

    // The top actually own the term AND are review-heavy → a real wall. Should be a NO.
    @Test
    fun realWall_topUseTermAndHeavyReviews_isNo() {
        val r = run(
            pop = 60,
            competitors = listOf(c(1, 1.0, reviews = 80_000), c(2, 1.0, reviews = 60_000), c(3, 1.0, reviews = 40_000)),
            label = "real wall (title + heavy reviews)",
        )
        assertEquals(OpportunityVerdict.NO, r.verdict)
    }

    // Big, well-reviewed apps that simply don't use the term aren't a wall for it → YES.
    @Test
    fun bigAppsButNobodyUsesTerm_isYes() {
        val r = run(
            pop = 55,
            competitors = listOf(c(1, 0.0, reviews = 90_000), c(2, 0.0, reviews = 70_000), c(3, 0.0, reviews = 50_000)),
            label = "big reviews, term unused",
        )
        assertEquals(OpportunityVerdict.YES, r.verdict)
    }

    // A genuine wall, but we grow reviews 3× faster than its median → we can climb → YES.
    @Test
    fun weOutVelocityARealWall_isYes() {
        val r = run(
            pop = 60,
            competitors = listOf(c(1, 1.0, vel = 200), c(2, 1.0, vel = 200), c(3, 1.0, vel = 200)),
            ourVel = 600,
            label = "wall but we out-velocity 3x",
        )
        assertEquals(OpportunityVerdict.YES, r.verdict)
        assertEquals(3.0, r.velocityAdvantage)
    }

    @Test
    fun lowVolume_isReserve() {
        assertEquals(
            OpportunityVerdict.RESERVE,
            run(pop = 3, competitors = listOf(c(1, 1.0, reviews = 100)), label = "low volume").verdict
        )
    }

    @Test
    fun noData_isUnknown() {
        assertEquals(
            OpportunityVerdict.UNKNOWN,
            run(pop = null, competitors = listOf(c(1, 1.0, reviews = 100)), label = "no popularity").verdict
        )
        assertEquals(
            OpportunityVerdict.UNKNOWN,
            run(pop = 50, competitors = emptyList(), label = "no competitors").verdict
        )
    }

    @Test
    fun titleFactor_matchesWordsAccentInsensitive() {
        assertEquals(1.0, OpportunityScorer.titleFactor("quoi manger", "Quoi Manger ?", null))
        assertEquals(1.0, OpportunityScorer.titleFactor("café", "Bon Café, Super Café", null))
        assertEquals(0.5, OpportunityScorer.titleFactor("pizza", "Smart Cook", "le meilleur pizza maker"))
        assertEquals(0.0, OpportunityScorer.titleFactor("pizza", "Cooking Fever", "jeu de cuisine"))
    }

    // A noisy least-squares slope can yield a negative velocity — it must floor to 0, never feed a
    // negative into log10 (which would give NaN and crash roundToInt). Regression test.
    @Test
    fun negativeVelocity_doesNotProduceNaN() {
        val r = run(
            pop = 50,
            competitors = listOf(c(1, 1.0, vel = -8), c(2, 1.0, vel = 5), c(3, 0.0, vel = -20)),
            label = "negative velocity",
        )
        assertTrue(r.score != null && !r.wallStrength.isNaN(), "must produce a finite score, got ${r.score}/${r.wallStrength}")
    }

    @Test
    fun wallStrength_zeroWhenTopDoesNotUseTerm() {
        assertEquals(0.0, OpportunityScorer.wallStrength(listOf(c(1, 0.0, reviews = 99_999))))
        assertTrue(OpportunityScorer.wallStrength(listOf(c(1, 1.0, reviews = 80_000))) > 0.9)
    }

}
