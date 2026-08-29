package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.keywords.CandidateStatus
import me.nathanfallet.asonar.domain.models.keywords.KeywordCandidate
import me.nathanfallet.asonar.domain.repositories.AppsRepository
import me.nathanfallet.asonar.domain.repositories.KeywordCandidatesRepository

class ListKeywordCandidatesUseCaseImpl(
    private val appsRepository: AppsRepository,
    private val keywordCandidatesRepository: KeywordCandidatesRepository,
) : ListKeywordCandidatesUseCase {

    override suspend fun invoke(
        appId: Long,
        statuses: Set<CandidateStatus>,
        minPopularity: Int?,
    ): List<KeywordCandidate>? {
        appsRepository.get(appId) ?: return null
        return keywordCandidatesRepository.list(appId, statuses)
            .filter { candidate ->
                minPopularity == null || (candidate.popularity ?: return@filter true) >= minPopularity
            }
    }

}
