package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.keywords.CompetitorSignal
import me.nathanfallet.asonar.domain.models.keywords.OpportunityVerdict

/**
 * The keyword "brain", **per store**. The wall/verdict math is shared (see [BaseOpportunityScorer]);
 * each store plugs in how a keyword's placement translates to usage strength ([titleFactor]) — the
 * App Store (subtitle at half, a repeated term across fields doesn't stack) and Play (title + short
 * description density, repetition adds up) weigh placement differently.
 *
 * Selected by store exactly like the other store-dependent services: inject `List<OpportunityScorer>`
 * and pick `firstOrNull { it.store == keyword.store }`. Adding Play = one more implementation, nothing
 * else to refactor.
 */
interface OpportunityScorer {

    val store: Store

    /** How strongly one app carries the keyword (0..1), per this store's placement rules. */
    fun titleFactor(term: String, title: String, subtitle: String?): Double

    /** Fraction of the top that carries the term in its title — for display. */
    fun titleShare(competitors: List<CompetitorSignal>): Double

    /** Score the opportunity from the captured signals. */
    fun score(inputs: Inputs): Result

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
}
