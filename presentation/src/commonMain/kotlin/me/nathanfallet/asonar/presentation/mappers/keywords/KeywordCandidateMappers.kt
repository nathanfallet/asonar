package me.nathanfallet.asonar.presentation.mappers.keywords

import me.nathanfallet.asonar.api.responses.keywords.KeywordCandidateResponse
import me.nathanfallet.asonar.domain.models.keywords.KeywordCandidate

/** Maps a [KeywordCandidate] to its wire form. */
fun KeywordCandidate.toKeywordCandidateResponse() = KeywordCandidateResponse(
    id = id,
    appId = appId,
    term = term,
    country = country,
    // Sorted so the same candidate always serializes the same way — a set has no order of its own.
    sources = sources.map { it.name }.sorted(),
    detail = detail,
    popularity = popularity,
    status = status.name,
    discoveredAt = discoveredAt,
    updatedAt = updatedAt,
)
