package me.nathanfallet.asonar.domain.models.application

/**
 * A slice of a listing: how many rows to return, from which offset, and an optional free-text
 * search. Shared by every repository that lists, so paging works the same everywhere.
 */
data class Pagination(
    val limit: Long = 25,
    val offset: Long = 0,
    val search: String? = null,
)
