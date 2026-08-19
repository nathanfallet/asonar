package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.snapshots.RankSnapshot

/** Lists the rank history of one of our apps on a keyword, newest first. */
interface ListRankHistoryUseCase {

    suspend operator fun invoke(keywordId: Long, appId: Long, pagination: Pagination): List<RankSnapshot>

}
