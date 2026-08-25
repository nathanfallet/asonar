package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.keywords.Keyword
import me.nathanfallet.asonar.domain.models.keywords.KeywordPayload
import me.nathanfallet.asonar.domain.repositories.KeywordsRepository

class GetOrCreateKeywordUseCaseImpl(
    private val keywordsRepository: KeywordsRepository,
    private val refreshKeywordUseCase: RefreshKeywordUseCase,
) : GetOrCreateKeywordUseCase {

    override suspend fun invoke(payload: KeywordPayload): Keyword {
        val normalized = payload.copy(
            term = payload.term.trim().lowercase(),
            country = payload.country.trim().uppercase(),
        )
        keywordsRepository.getByTerm(normalized.term, normalized.store, normalized.country)
            ?.let { return it }
        // First time we track this term: kick off a fetch right away (same effect as hitting
        // refresh) so the keyword isn't left empty until someone refreshes it by hand.
        return keywordsRepository.create(normalized).also { refreshKeywordUseCase(it.id) }
    }

}
