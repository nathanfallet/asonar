package me.nathanfallet.asonar.presentation.mappers.keywords

import me.nathanfallet.asonar.api.responses.keywords.KeywordResponse
import me.nathanfallet.asonar.domain.models.keywords.KeywordOverview

/** Maps a [KeywordOverview] to its wire form. */
fun KeywordOverview.toKeywordResponse() = KeywordResponse(
    id = keyword.id,
    term = keyword.term,
    store = keyword.store.name,
    country = keyword.country,
    createdAt = keyword.createdAt,
    latestPopularity = latestPopularity?.popularity,
    latestPopularityAt = latestPopularity?.capturedAt,
)
