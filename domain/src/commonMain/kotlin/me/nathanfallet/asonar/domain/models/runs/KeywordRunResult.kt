package me.nathanfallet.asonar.domain.models.runs

import kotlinx.serialization.Serializable
import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshot
import me.nathanfallet.asonar.domain.models.snapshots.RankSnapshot
import me.nathanfallet.asonar.domain.models.snapshots.TopAppSnapshot

/** What recording a run wrote — the snapshots created, echoed back to the caller. */
@Serializable
data class KeywordRunResult(
    val popularity: PopularitySnapshot? = null,
    val ranks: List<RankSnapshot> = emptyList(),
    val topApps: List<TopAppSnapshot> = emptyList(),
)
