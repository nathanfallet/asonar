package me.nathanfallet.asonar.domain.repositories

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.apps.Store
import me.nathanfallet.asonar.domain.models.keywords.Keyword
import me.nathanfallet.asonar.domain.models.keywords.KeywordPayload

/** Reads and writes the tracked keywords. */
interface KeywordsRepository {

    /** Lists the tracked keywords, newest first. */
    suspend fun list(pagination: Pagination): List<Keyword>

    /** Reads a keyword by its identifier. */
    suspend fun get(id: Long): Keyword?

    /** Reads a keyword by its (term, store, country) identity, used for get-or-create. */
    suspend fun getByTerm(term: String, store: Store, country: String): Keyword?

    /** Starts tracking a keyword. */
    suspend fun create(payload: KeywordPayload): Keyword

    /** Stops tracking a keyword. @return True if a row was deleted. */
    suspend fun delete(id: Long): Boolean

}
