package me.nathanfallet.asonar.domain.models.apps

import kotlinx.serialization.Serializable

/**
 * Why an [App] is followed. Both roles are tracked identically — a fetch records the rank of *every*
 * registered app on the store, so a competitor gets its ranking and rating history for free — the
 * role only says what we do with it:
 *
 * - [OWNED]: an app we optimize. Opportunities are scored *for* it (our velocity vs the leaders').
 * - [COMPETITOR]: an app we watch. Its ranks answer "where do they beat us", and its metadata and
 *   reviews are a source of candidate keywords (the terms *they* index).
 *
 * Its name is what gets stored in the database.
 */
@Serializable
enum class AppRole {
    OWNED,
    COMPETITOR,
}
