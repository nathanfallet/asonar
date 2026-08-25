package me.nathanfallet.asonar.presentation.mappers.apps

import me.nathanfallet.asonar.api.responses.apps.AppKeywordCoverageResponse
import me.nathanfallet.asonar.api.responses.apps.KeywordCoverageEntryResponse
import me.nathanfallet.asonar.api.responses.apps.RankPointResponse
import me.nathanfallet.asonar.domain.models.apps.AppKeywordCoverage
import me.nathanfallet.asonar.domain.models.apps.KeywordCoverageEntry
import me.nathanfallet.asonar.presentation.mappers.keywords.toKeywordResponse

fun AppKeywordCoverage.toAppKeywordCoverageResponse() = AppKeywordCoverageResponse(
    app = app.toAppResponse(),
    entries = entries.map { it.toCoverageEntryResponse() },
)

fun KeywordCoverageEntry.toCoverageEntryResponse() = KeywordCoverageEntryResponse(
    keyword = keyword.toKeywordResponse(),
    popularity = popularity,
    rank = rank,
    totalResults = totalResults,
    capturedAt = capturedAt,
    history = history.map { RankPointResponse(it.rank, it.capturedAt) },
)
