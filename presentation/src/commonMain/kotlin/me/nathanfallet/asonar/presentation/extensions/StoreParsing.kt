package me.nathanfallet.asonar.presentation.extensions

import me.nathanfallet.asonar.domain.models.apps.Store

/** Parses a store name (e.g. "APP_STORE") to a [Store], case-insensitively, or null if unknown. */
fun parseStore(name: String): Store? =
    Store.entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
