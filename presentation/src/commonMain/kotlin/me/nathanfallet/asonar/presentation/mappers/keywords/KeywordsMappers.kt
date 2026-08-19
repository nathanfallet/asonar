package me.nathanfallet.asonar.presentation.mappers.keywords

import me.nathanfallet.asonar.api.responses.keywords.KeywordAppRankResponse
import me.nathanfallet.asonar.api.responses.keywords.KeywordDetailResponse
import me.nathanfallet.asonar.api.responses.keywords.KeywordResponse
import me.nathanfallet.asonar.domain.models.keywords.Keyword
import me.nathanfallet.asonar.domain.models.keywords.KeywordAppRank
import me.nathanfallet.asonar.domain.models.keywords.KeywordDetail
import me.nathanfallet.asonar.domain.models.keywords.KeywordOverview
import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshot
import me.nathanfallet.asonar.presentation.mappers.apps.toAppResponse
import me.nathanfallet.asonar.presentation.mappers.snapshots.toRankSnapshotResponse
import me.nathanfallet.asonar.presentation.mappers.snapshots.toTopAppSnapshotResponse

/** Maps a [Keyword] to its wire form, folding in a latest popularity reading when known. */
fun Keyword.toKeywordResponse(latestPopularity: PopularitySnapshot? = null) = KeywordResponse(
    id = id,
    term = term,
    store = store.name,
    country = country,
    createdAt = createdAt,
    latestPopularity = latestPopularity?.popularity,
    latestPopularityAt = latestPopularity?.capturedAt,
)

fun KeywordOverview.toKeywordResponse() = keyword.toKeywordResponse(latestPopularity)

fun KeywordDetail.toKeywordDetailResponse() = KeywordDetailResponse(
    keyword = keyword.toKeywordResponse(latestPopularity),
    topApps = topApps.map { it.toTopAppSnapshotResponse() },
    ranks = ranks.map { it.toKeywordAppRankResponse() },
)

fun KeywordAppRank.toKeywordAppRankResponse() = KeywordAppRankResponse(
    app = app.toAppResponse(),
    rank = rank.toRankSnapshotResponse(),
)
