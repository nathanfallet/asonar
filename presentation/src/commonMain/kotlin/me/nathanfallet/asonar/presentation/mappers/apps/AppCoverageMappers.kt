package me.nathanfallet.asonar.presentation.mappers.apps

import me.nathanfallet.asonar.api.responses.apps.AppKeywordCoverageResponse
import me.nathanfallet.asonar.api.responses.apps.CoverageSummaryResponse
import me.nathanfallet.asonar.api.responses.apps.KeywordCoverageEntryResponse
import me.nathanfallet.asonar.api.responses.apps.RankPointResponse
import me.nathanfallet.asonar.domain.models.apps.AppKeywordCoverage
import me.nathanfallet.asonar.domain.models.apps.CoverageSummary
import me.nathanfallet.asonar.domain.models.apps.KeywordCoverageEntry
import me.nathanfallet.asonar.presentation.mappers.keywords.toKeywordResponse

fun AppKeywordCoverage.toAppKeywordCoverageResponse() = AppKeywordCoverageResponse(
    app = app.toAppResponse(),
    summary = summary.toCoverageSummaryResponse(),
    entries = entries.map { it.toCoverageEntryResponse() },
)

fun CoverageSummary.toCoverageSummaryResponse() = CoverageSummaryResponse(
    averageRank = averageRank,
    bestRank = bestRank,
    worstRank = worstRank,
    rankedCount = rankedCount,
    trackedCount = trackedCount,
    top5 = top5,
    top25 = top25,
    top100 = top100,
    beyond100 = beyond100,
    wentUp = wentUp,
    wentDown = wentDown,
    unchanged = unchanged,
)

fun KeywordCoverageEntry.toCoverageEntryResponse() = KeywordCoverageEntryResponse(
    keyword = keyword.toKeywordResponse(),
    popularity = popularity,
    rank = rank,
    totalResults = totalResults,
    capturedAt = capturedAt,
    history = history.map { RankPointResponse(it.rank, it.capturedAt) },
)
