package me.nathanfallet.asonar.domain.models.application

import kotlinx.serialization.Serializable

/**
 * A slice of a listing: how many rows to return, from which offset, and an optional free-text
 * search. Shared by every repository that lists, so paging works the same everywhere.
 *
 * [limit] `<= 0` means **no limit** (return everything) — a stopgap for the keyword list until real
 * pagination lands (see ROADMAP); don't lean on it for large tables.
 */
@Serializable
data class Pagination(
    val limit: Long = 25,
    val offset: Long = 0,
    val search: String? = null,
)
