package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.keywords.Keyword
import me.nathanfallet.asonar.domain.models.keywords.KeywordPayload
import me.nathanfallet.asonar.domain.repositories.KeywordsRepository

class GetOrCreateKeywordUseCaseImpl(
    private val keywordsRepository: KeywordsRepository,
) : GetOrCreateKeywordUseCase {

    override suspend fun invoke(payload: KeywordPayload): Keyword {
        val normalized = payload.copy(
            term = payload.term.trim().lowercase(),
            country = payload.country.trim().uppercase(),
        )
        return keywordsRepository.getByTerm(normalized.term, normalized.store, normalized.country)
            ?: keywordsRepository.create(normalized)
    }

}
