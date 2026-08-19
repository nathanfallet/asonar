package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.repositories.KeywordsRepository

class DeleteKeywordUseCaseImpl(
    private val keywordsRepository: KeywordsRepository,
) : DeleteKeywordUseCase {

    override suspend fun invoke(id: Long): Boolean = keywordsRepository.delete(id)

}
