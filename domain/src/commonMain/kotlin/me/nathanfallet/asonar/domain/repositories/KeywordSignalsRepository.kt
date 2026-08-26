package me.nathanfallet.asonar.domain.repositories

import me.nathanfallet.asonar.domain.models.keywords.KeywordSignals
import me.nathanfallet.asonar.domain.models.keywords.KeywordSignalsPayload

/** Records and reads the precomputed opportunity signals for keywords (append-only). */
interface KeywordSignalsRepository {

    /** Records a signals reading. */
    suspend fun create(payload: KeywordSignalsPayload): KeywordSignals

    /** The most recent signals for a keyword, or null if none captured yet. */
    suspend fun getLatestForKeyword(keywordId: Long): KeywordSignals?

}
