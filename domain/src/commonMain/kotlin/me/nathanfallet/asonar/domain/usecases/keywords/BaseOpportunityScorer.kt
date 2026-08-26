package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.keywords.CompetitorSignal
import me.nathanfallet.asonar.domain.models.keywords.OpportunityVerdict
import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * Shared, store-agnostic scoring math: the **wall** aggregation, review strength, verdict thresholds
 * and final score. It runs on the precomputed per-competitor [CompetitorSignal.titleFactor] — each
 * store computes that placement strength its own way ([OpportunityScorer.titleFactor]), so everything
 * downstream is common. Thresholds/weights are `open` so a store can tune them without duplicating the
 * math.
 *
 * The heart is **wall strength**: how strongly the *top* of the results is held. An app only walls a
 * keyword if it BOTH uses the term AND has review weight, and the top positions count far more (a weak
 * #1 opens the whole keyword). A #1 that doesn't use the term, or uses it with few reviews, yields a
 * low wall we can pass — even when bigger apps sit lower down.
 */
abstract class BaseOpportunityScorer : OpportunityScorer {

    // Verdict thresholds (open — a store can tune them).
    protected open val popMin = 5             // below this, too little volume to prioritise
    protected open val wallHigh = 0.50        // the top is strongly held → a wall
    protected open val wallLow = 0.30         // the top is weakly held → an opening
    protected open val velBehind = 0.8        // we grow reviews clearly slower than the leaders
    protected open val velAhead = 1.2         // we grow reviews clearly faster → we can climb
    protected open val fewResults = 40        // a thin field is easy to break into

    // Score weights (sum to 1) + shaping constants (open).
    protected open val wWall = 0.6
    protected open val wVelocity = 0.3
    protected open val wResults = 0.1
    protected open val velFloor = 1.0         // avoid div-by-zero when leaders barely get reviews
    protected open val resultsCap = 400.0
    protected open val velocityCap = 1_000.0  // ratings/30d at which defender strength saturates

    override fun score(inputs: OpportunityScorer.Inputs): OpportunityScorer.Result {
        val medianVelocity = median(inputs.competitors.mapNotNull { it.ratingsPer30d })
        val velAdvantage = inputs.ourVelocity?.let { ours ->
            medianVelocity?.let { theirs -> ours / maxOf(theirs.toDouble(), velFloor) }
        }
        val wall = wallStrength(inputs.competitors)

        if (inputs.popularity == null || inputs.competitors.isEmpty()) {
            return OpportunityScorer.Result(OpportunityVerdict.UNKNOWN, null, wall, medianVelocity, velAdvantage)
        }

        // We size a wall by the term-carriers' review MOMENTUM (30-day velocity), not their static
        // review count: a stale giant is beatable, a fast climber isn't. Until that velocity exists
        // (needs a couple of days of history) we can't tell the two apart, so it's UNKNOWN — and the
        // background refresh keeps re-fetching it. A below-floor popularity is terminal and wins first
        // (no point waiting on a term nobody searches).
        if (inputs.popularity > popMin && inputs.competitors.any { it.titleFactor > 0.0 && it.ratingsPer30d == null }) {
            return OpportunityScorer.Result(OpportunityVerdict.UNKNOWN, null, wall, medianVelocity, velAdvantage)
        }

        val thin = (inputs.totalResults ?: Int.MAX_VALUE) <= fewResults

        val verdict = when {
            inputs.popularity <= popMin -> OpportunityVerdict.RESERVE
            wall >= wallHigh && (velAdvantage == null || velAdvantage < velBehind) -> OpportunityVerdict.NO
            wall <= wallLow || thin || (velAdvantage != null && velAdvantage >= velAhead) -> OpportunityVerdict.YES
            else -> OpportunityVerdict.YES_BUT
        }

        val wVel = velAdvantage?.let { (it / 2.0).coerceIn(0.0, 1.0) } ?: 0.5
        val wRes = (1.0 - (inputs.totalResults ?: resultsCap.toInt()) / resultsCap).coerceIn(0.0, 1.0)
        val winnability = wWall * (1.0 - wall) + wVelocity * wVel + wResults * wRes
        val score = (inputs.popularity * winnability).roundToInt().coerceIn(0, 100)

        return OpportunityScorer.Result(verdict, score, wall, medianVelocity, velAdvantage)
    }

    /**
     * 0..1 — how strongly the top is held. Each competitor's hold is `titleFactor × reviewStrength ×
     * 1/position` (an app that doesn't use the term, factor 0, or has no reviews holds nothing; the
     * top positions count far more). The wall is the **max** of the position-weighted *average* and
     * the *single strongest holder* — so a lone-but-total defender at the very top (e.g. Instagram
     * owning "instagram" while the nine apps below ignore the term) still walls the keyword instead of
     * being averaged away into a false opening.
     */
    fun wallStrength(competitors: List<CompetitorSignal>): Double {
        if (competitors.isEmpty()) return 0.0
        var num = 0.0
        var den = 0.0
        var strongestHold = 0.0
        for (c in competitors) {
            val weight = 1.0 / c.position
            val hold = c.titleFactor * reviewStrength(c) * weight
            num += hold
            den += weight
            strongestHold = maxOf(strongestHold, hold)
        }
        val averaged = if (den == 0.0) 0.0 else num / den
        return maxOf(averaged, strongestHold)
    }

    /**
     * 0..1 defender strength from the recent **30-day review velocity** — our best proxy for the
     * download velocity that actually drives App Store rank. A big *old* review total is deliberately
     * NOT used: ratings have no direct rank correlation (cf. app-aso ranking mechanics), and recent
     * review velocity is far more correlated than a stale total. Missing velocity is caught a level up
     * (→ UNKNOWN), so a null here only ever belongs to a non-term-carrier (titleFactor 0) whose wall
     * contribution is 0 anyway.
     */
    private fun reviewStrength(c: CompetitorSignal): Double =
        logStrength(c.ratingsPer30d ?: 0, velocityCap)

    // coerceAtLeast(0): a velocity can come out negative (noisy regression on a declining count) —
    // a shrinking app is weak, so floor it at 0 rather than feeding a negative into log10 (→ NaN).
    private fun logStrength(value: Int, cap: Double): Double =
        (log10(value.coerceAtLeast(0) + 1.0) / log10(cap)).coerceIn(0.0, 1.0)

    /** Median of a list of ints, rounded for even sizes. Null on empty. */
    fun median(values: List<Int>): Int? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else ((sorted[mid - 1] + sorted[mid]) / 2.0).roundToInt()
    }

    /** Weighted share of the top used only for display (fraction with the term in title). */
    override fun titleShare(competitors: List<CompetitorSignal>): Double =
        if (competitors.isEmpty()) 0.0 else competitors.count { it.titleFactor >= 1.0 }.toDouble() / competitors.size

    /** Lowercase + strip the common French accents so "quoi manger" matches "Quoi Manger ?". */
    protected fun fold(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s.lowercase()) {
            sb.append(
                when (c) {
                    'à', 'â', 'ä' -> 'a'
                    'é', 'è', 'ê', 'ë' -> 'e'
                    'î', 'ï' -> 'i'
                    'ô', 'ö' -> 'o'
                    'ù', 'û', 'ü' -> 'u'
                    'ç' -> 'c'
                    else -> c
                }
            )
        }
        return sb.toString()
    }
}
