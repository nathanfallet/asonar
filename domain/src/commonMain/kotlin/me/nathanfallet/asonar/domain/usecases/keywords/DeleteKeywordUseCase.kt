package me.nathanfallet.asonar.domain.usecases.keywords

/** Stops tracking a keyword. */
interface DeleteKeywordUseCase {

    /** @return True if a keyword was deleted. */
    suspend operator fun invoke(id: Long): Boolean

}
