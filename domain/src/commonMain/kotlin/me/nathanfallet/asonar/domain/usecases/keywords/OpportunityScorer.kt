package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.keywords.CompetitorSignal
import me.nathanfallet.asonar.domain.models.keywords.OpportunityVerdict
import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * The pure scoring core of the keyword "brain" — no I/O, so the ASO rules can be unit-tested and the
 * thresholds/weights tuned (or A/B'd) in one place. Data-gathering lives in the use case.
 *
 * The heart is **wall strength**: how strongly the *top* of the results is held. An app only walls a
 * keyword if it BOTH uses the term (title, or subtitle at half) AND has review weight — and the top
 * positions count far more (a weak #1 opens the whole keyword). So a #1 that doesn't use the term, or
 * uses it but has few reviews, yields a low wall we can pass, even when bigger apps sit lower down.
 */
internal object OpportunityScorer {

    data class Inputs(
        val popularity: Int?,                       // search volume 0-100
        val competitors: List<CompetitorSignal>,    // the captured top-of-results
        val ourVelocity: Int?,                      // our ratings/30d in this market
        val totalResults: Int?,
    )

    data class Result(
        val verdict: OpportunityVerdict,
        val score: Int?,
        val wallStrength: Double,
        val top10MedianVelocity: Int?,
        val velocityAdvantage: Double?,
    )

    // Verdict thresholds (tunable).
    private const val POP_MIN = 5             // below this, too little volume to prioritise
    private const val WALL_HIGH = 0.50        // the top is strongly held → a wall
    private const val WALL_LOW = 0.30         // the top is weakly held → an opening
    private const val VEL_BEHIND = 0.8        // we grow reviews clearly slower than the leaders
    private const val VEL_AHEAD = 1.2         // we grow reviews clearly faster → we can climb
    private const val FEW_RESULTS = 40        // a thin field is easy to break into

    // Score weights (sum to 1) + shaping constants (tunable).
    private const val W_WALL = 0.6
    private const val W_VELOCITY = 0.3
    private const val W_RESULTS = 0.1
    private const val VEL_FLOOR = 1.0         // avoid div-by-zero when leaders barely get reviews
    private const val RESULTS_CAP = 400.0
    private const val TOTAL_CAP = 50_000.0    // ratingCount at which review strength saturates
    private const val VELOCITY_CAP = 1_000.0  // ratings/30d at which review strength saturates

    fun score(inp: Inputs): Result {
        val medianVelocity = median(inp.competitors.mapNotNull { it.ratingsPer30d })
        val velAdvantage = inp.ourVelocity?.let { ours ->
            medianVelocity?.let { theirs -> ours / maxOf(theirs.toDouble(), VEL_FLOOR) }
        }
        val wall = wallStrength(inp.competitors)

        if (inp.popularity == null || inp.competitors.isEmpty()) {
            return Result(OpportunityVerdict.UNKNOWN, null, wall, medianVelocity, velAdvantage)
        }

        val fewResults = (inp.totalResults ?: Int.MAX_VALUE) <= FEW_RESULTS

        val verdict = when {
            inp.popularity <= POP_MIN -> OpportunityVerdict.RESERVE
            wall >= WALL_HIGH && (velAdvantage == null || velAdvantage < VEL_BEHIND) -> OpportunityVerdict.NO
            wall <= WALL_LOW || fewResults || (velAdvantage != null && velAdvantage >= VEL_AHEAD) -> OpportunityVerdict.YES
            else -> OpportunityVerdict.YES_BUT
        }

        val wVel = velAdvantage?.let { (it / 2.0).coerceIn(0.0, 1.0) } ?: 0.5
        val wResults = (1.0 - (inp.totalResults ?: RESULTS_CAP.toInt()) / RESULTS_CAP).coerceIn(0.0, 1.0)
        val winnability = W_WALL * (1.0 - wall) + W_VELOCITY * wVel + W_RESULTS * wResults
        val score = (inp.popularity * winnability).roundToInt().coerceIn(0, 100)

        return Result(verdict, score, wall, medianVelocity, velAdvantage)
    }

    /**
     * 0..1 — how strongly the top is held. Position-weighted average (a la 1/rank, so #1 dominates)
     * of each competitor's `titleFactor × reviewStrength`: an app that doesn't use the term (factor 0)
     * or has no reviews contributes nothing to the wall, regardless of where it sits.
     */
    fun wallStrength(competitors: List<CompetitorSignal>): Double {
        if (competitors.isEmpty()) return 0.0
        var num = 0.0
        var den = 0.0
        for (c in competitors) {
            val weight = 1.0 / c.position
            num += c.titleFactor * reviewStrength(c) * weight
            den += weight
        }
        return if (den == 0.0) 0.0 else num / den
    }

    /** 0..1 review weight: recent velocity when we have it, else total ratings, both log-saturated. */
    private fun reviewStrength(c: CompetitorSignal): Double =
        c.ratingsPer30d?.let { logStrength(it, VELOCITY_CAP) } ?: logStrength(c.ratingCount ?: 0, TOTAL_CAP)

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
    fun titleShare(competitors: List<CompetitorSignal>): Double =
        if (competitors.isEmpty()) 0.0 else competitors.count { it.titleFactor >= 1.0 }.toDouble() / competitors.size

    /**
     * How strongly one app carries the keyword: 1.0 when every word of the term is in the title, 0.5
     * when only in title+subtitle together, 0 otherwise. Apple indexes words, so we match word-by-word
     * (case/accent-folded), not the exact phrase.
     */
    fun titleFactor(term: String, title: String, subtitle: String?): Double {
        val words = fold(term).split(' ').filter { it.length >= 2 }
        if (words.isEmpty()) return 0.0
        val t = fold(title)
        val ts = fold(title + " " + (subtitle ?: ""))
        return when {
            words.all { t.contains(it) } -> 1.0
            words.all { ts.contains(it) } -> 0.5
            else -> 0.0
        }
    }

    /** Lowercase + strip the common French accents so "quoi manger" matches "Quoi Manger ?". */
    private fun fold(s: String): String {
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
