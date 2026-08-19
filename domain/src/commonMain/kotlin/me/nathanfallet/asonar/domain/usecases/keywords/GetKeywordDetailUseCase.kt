package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.keywords.KeywordDetail

/** Reads the full current picture of a keyword: popularity, top-of-results and our apps' ranks. */
interface GetKeywordDetailUseCase {

    suspend operator fun invoke(keywordId: Long): KeywordDetail?

}
