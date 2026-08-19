package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.keywords.Keyword
import me.nathanfallet.asonar.domain.models.keywords.KeywordPayload

/**
 * Returns the tracked keyword for a (term, store, country), starting to track it the first time it
 * is seen. The term is normalized (trimmed, lower-cased) and the country upper-cased before lookup,
 * so "TDAH Repas " and "tdah repas" resolve to the same row rather than two.
 */
interface GetOrCreateKeywordUseCase {

    suspend operator fun invoke(payload: KeywordPayload): Keyword

}
