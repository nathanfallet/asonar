package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.snapshots.TopAppSnapshot

/** Reads a keyword's most recent top-of-results (the latest observation, ordered by position). */
interface GetLatestTopAppsUseCase {

    suspend operator fun invoke(keywordId: Long): List<TopAppSnapshot>

}
