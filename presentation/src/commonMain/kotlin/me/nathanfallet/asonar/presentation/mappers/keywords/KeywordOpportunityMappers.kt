package me.nathanfallet.asonar.presentation.mappers.keywords

import me.nathanfallet.asonar.api.responses.keywords.KeywordOpportunityResponse
import me.nathanfallet.asonar.domain.models.keywords.KeywordOpportunity

fun KeywordOpportunity.toKeywordOpportunityResponse() = KeywordOpportunityResponse(
    keyword = keyword.toKeywordResponse(),
    verdict = verdict.name,
    score = score,
    popularity = popularity,
    ourRank = ourRank,
    ourRatingsPer30d = ourRatingsPer30d,
    top10MedianRatingsPer30d = top10MedianRatingsPer30d,
    velocityAdvantage = velocityAdvantage,
    wallStrength = wallStrength,
    top10TitleUsage = top10TitleUsage,
    totalResults = totalResults,
    comment = comment,
)
