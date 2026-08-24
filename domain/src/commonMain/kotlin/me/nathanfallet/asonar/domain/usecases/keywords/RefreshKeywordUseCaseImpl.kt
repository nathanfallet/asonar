package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.repositories.KeywordsRepository
import me.nathanfallet.asonar.domain.services.KeywordFetchQueue

class RefreshKeywordUseCaseImpl(
    private val keywordsRepository: KeywordsRepository,
    private val keywordFetchQueue: KeywordFetchQueue,
) : RefreshKeywordUseCase {

    override suspend fun invoke(keywordId: Long): Boolean {
        keywordsRepository.get(keywordId) ?: return false
        keywordFetchQueue.enqueueFetch(keywordId)
        return true
    }

}
