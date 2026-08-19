package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshot

/** Lists a keyword's popularity history, newest first. */
interface ListPopularityHistoryUseCase {

    suspend operator fun invoke(keywordId: Long, pagination: Pagination): List<PopularitySnapshot>

}
