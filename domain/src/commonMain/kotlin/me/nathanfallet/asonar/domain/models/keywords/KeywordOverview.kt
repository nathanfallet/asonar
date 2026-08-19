package me.nathanfallet.asonar.domain.models.keywords

import kotlinx.serialization.Serializable
import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshot

/** A tracked keyword paired with its most recent popularity reading (null if never fetched). */
@Serializable
data class KeywordOverview(
    val keyword: Keyword,
    val latestPopularity: PopularitySnapshot? = null,
)
