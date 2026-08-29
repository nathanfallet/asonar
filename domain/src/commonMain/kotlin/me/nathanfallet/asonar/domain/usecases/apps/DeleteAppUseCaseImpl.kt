package me.nathanfallet.asonar.domain.usecases.apps

import me.nathanfallet.asonar.domain.repositories.AppsRepository
import me.nathanfallet.asonar.domain.repositories.KeywordCandidatesRepository

class DeleteAppUseCaseImpl(
    private val appsRepository: AppsRepository,
    private val keywordCandidatesRepository: KeywordCandidatesRepository,
) : DeleteAppUseCase {

    override suspend fun invoke(id: Long): Boolean {
        // Candidates belong to the app they were discovered for and mean nothing without it — unlike
        // the snapshots, which stay as history. Dropped first so a failure can't orphan them.
        keywordCandidatesRepository.deleteForApp(id)
        return appsRepository.delete(id)
    }

}
