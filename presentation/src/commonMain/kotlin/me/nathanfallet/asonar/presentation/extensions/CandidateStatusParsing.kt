package me.nathanfallet.asonar.presentation.extensions

import me.nathanfallet.asonar.domain.models.keywords.CandidateStatus

/**
 * Parses a comma-separated list of candidate states (e.g. "NEW,ADDED") case-insensitively. A missing
 * or blank value means the pending ones — reviewing is about what hasn't been decided yet. Returns
 * null if any name given isn't a state, so the caller can reject the whole filter rather than
 * silently listing the wrong thing.
 */
fun parseCandidateStatuses(raw: String?): Set<CandidateStatus>? {
    val names = raw?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
    if (names.isEmpty()) return setOf(CandidateStatus.NEW)
    return names
        .map { name -> CandidateStatus.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: return null }
        .toSet()
}
