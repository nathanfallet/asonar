package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.keywords.KeywordOverview

/** Lists the tracked keywords, each with its latest popularity — the data behind the dashboard. */
interface ListKeywordOverviewsUseCase {

    suspend operator fun invoke(pagination: Pagination): List<KeywordOverview>

}
