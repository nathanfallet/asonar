package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.keywords.CompetitorSignal
import me.nathanfallet.asonar.domain.models.keywords.OpportunityVerdict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Calibrates the App Store "brain" against the app-aso opportunity shortcut (§3bis): the wall crosses
 * **title usage** with the top's **recent 30-day review velocity** (the proxy for download velocity,
 * which drives rank — a stale review total does not). Without that velocity we can't judge a defender,
 * so it's UNKNOWN, never a guess from the review count.
 */
class OpportunityScorerTest {

    private val scorer = AppStoreOpportunityScorer()

    // position, title factor (1 title / .5 subtitle / 0 none), 30-day review velocity
    private fun c(pos: Int, title: Double, vel: Int? = null) = CompetitorSignal(pos, title, null, vel)

    private fun run(
        pop: Int? = 50,
        competitors: List<CompetitorSignal>,
        ourVel: Int? = null,
        total: Int? = 1000,
        label: String = "",
    ): OpportunityScorer.Result {
        val r = scorer.score(OpportunityScorer.Inputs(pop, competitors, ourVel, total))
        println("[scorer] ${label.padEnd(36)} verdict=${r.verdict} score=${r.score} wall=${(r.wallStrength * 100).toInt()}%")
        return r
    }

    // Nobody in the top uses the term → no textual wall to break → YES. (No velocity needed: there's
    // no defender to size up.)
    @Test
    fun nobodyUsesTerm_isYes() {
        val r = run(pop = 55, competitors = listOf(c(1, 0.0), c(2, 0.0), c(3, 0.0)), label = "term unused")
        assertEquals(OpportunityVerdict.YES, r.verdict)
    }

    // The top use the term but gain reviews slowly (weak momentum) → beatable → YES.
    @Test
    fun topUsesTermButLowVelocity_isYes() {
        val r = run(
            pop = 55,
            competitors = listOf(c(1, 1.0, vel = 3), c(2, 1.0, vel = 2), c(3, 0.0)),
            label = "term in title, low velocity",
        )
        assertEquals(OpportunityVerdict.YES, r.verdict)
    }

    // The top own the term AND gain reviews fast (strong momentum) → a real wall → NO.
    @Test
    fun topUsesTermAndHighVelocity_isNo() {
        val r = run(
            pop = 60,
            competitors = listOf(c(1, 1.0, vel = 800), c(2, 1.0, vel = 600), c(3, 1.0, vel = 400)),
            label = "term in title, high velocity",
        )
        assertEquals(OpportunityVerdict.NO, r.verdict)
    }

    // A term-carrier at the top has no 30-day velocity yet (fresh history) → we can't tell a strong
    // defender from a stale one → UNKNOWN (never a guess from the review count). The background refresh
    // keeps re-fetching until the velocity accrues.
    @Test
    fun termCarrierWithoutVelocity_isUnknown() {
        val r = run(
            pop = 100,
            competitors = listOf(c(1, 1.0, vel = null), c(2, 0.0, vel = null)),
            label = "term-carrier, no velocity",
        )
        assertEquals(OpportunityVerdict.UNKNOWN, r.verdict)
        assertEquals(null, r.score)
    }

    // A single dominant #1 that owns the term with strong velocity walls the keyword even when the nine
    // apps below ignore the term — you won't dethrone it. Averaging alone would dilute that lone-but-
    // total defender (the "instagram" case); the wall reflects the strongest holder → NO.
    @Test
    fun dominantTopHolder_restIgnoreTerm_isNo() {
        val competitors = listOf(c(1, 1.0, vel = 900)) + (2..10).map { c(it, 0.0) }
        val r = run(pop = 100, competitors = competitors, label = "dominant #1, rest ignore term")
        assertEquals(OpportunityVerdict.NO, r.verdict)
        assertTrue(r.wallStrength > 0.9, "a dominant #1 should wall the keyword, got ${r.wallStrength}")
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

    // Below the popularity floor is terminal — Réserve wins even over a missing velocity (no point
    // waiting on a term nobody searches).
    @Test
    fun lowVolume_isReserve() {
        assertEquals(
            OpportunityVerdict.RESERVE,
            run(pop = 3, competitors = listOf(c(1, 1.0, vel = null)), label = "low volume").verdict
        )
    }

    @Test
    fun noData_isUnknown() {
        assertEquals(
            OpportunityVerdict.UNKNOWN,
            run(pop = null, competitors = listOf(c(1, 1.0, vel = 100)), label = "no popularity").verdict
        )
        assertEquals(
            OpportunityVerdict.UNKNOWN,
            run(pop = 50, competitors = emptyList(), label = "no competitors").verdict
        )
    }

    @Test
    fun titleFactor_matchesWordsAccentInsensitive() {
        assertEquals(1.0, scorer.titleFactor("quoi manger", "Quoi Manger ?", null))
        assertEquals(1.0, scorer.titleFactor("café", "Bon Café, Super Café", null))
        assertEquals(0.5, scorer.titleFactor("pizza", "Smart Cook", "le meilleur pizza maker"))
        assertEquals(0.0, scorer.titleFactor("pizza", "Cooking Fever", "jeu de cuisine"))
    }

    // A noisy least-squares slope can yield a negative velocity — it must floor to 0, never feed a
    // negative into log10 (which would give NaN and crash roundToInt). Regression test.
    @Test
    fun negativeVelocity_doesNotProduceNaN() {
        val r = run(
            pop = 50,
            competitors = listOf(c(1, 1.0, vel = -8), c(2, 1.0, vel = 5), c(3, 0.0)),
            label = "negative velocity",
        )
        assertTrue(r.score != null && !r.wallStrength.isNaN(), "must produce a finite score, got ${r.score}/${r.wallStrength}")
    }

    @Test
    fun wallStrength_zeroWhenTopDoesNotUseTerm() {
        assertEquals(0.0, scorer.wallStrength(listOf(c(1, 0.0, vel = 900))))
        assertTrue(scorer.wallStrength(listOf(c(1, 1.0, vel = 900))) > 0.9)
    }
}
