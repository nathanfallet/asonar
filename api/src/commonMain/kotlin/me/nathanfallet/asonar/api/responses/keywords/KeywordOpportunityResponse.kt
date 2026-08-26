package me.nathanfallet.asonar.api.responses.keywords

import kotlinx.serialization.Serializable

/** One keyword scored as an ASO opportunity for an app. [verdict] is YES / YES_BUT / NO / RESERVE / UNKNOWN. */
@Serializable
data class KeywordOpportunityResponse(
    val keyword: KeywordResponse,
    val verdict: String,
    val score: Int? = null,
    val popularity: Int? = null,
    val ourRank: Int? = null,
    val ourRatingsPer30d: Int? = null,
    val top10MedianRatingsPer30d: Int? = null,
    val velocityAdvantage: Double? = null,
    val wallStrength: Double = 0.0,
    val top10TitleUsage: Double = 0.0,
    val totalResults: Int? = null,
    val comment: String = "",
)

/** An app's keyword recommendations: every tracked keyword scored, best opportunity first. */
@Serializable
data class KeywordOpportunitiesResponse(
    val opportunities: List<KeywordOpportunityResponse>,
)
