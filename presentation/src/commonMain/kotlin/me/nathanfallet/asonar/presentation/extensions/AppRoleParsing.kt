package me.nathanfallet.asonar.presentation.extensions

import me.nathanfallet.asonar.domain.models.apps.AppRole

/**
 * Parses a role name (e.g. "COMPETITOR") to an [AppRole], case-insensitively. A missing/blank value
 * means [AppRole.OWNED] — registering an app without saying otherwise registers one of ours. Returns
 * null only for a value that was given but isn't a role, so the caller can reject it.
 */
fun parseAppRole(name: String?): AppRole? {
    val trimmed = name?.trim().orEmpty()
    if (trimmed.isEmpty()) return AppRole.OWNED
    return AppRole.entries.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
}
