package me.nathanfallet.asonar.domain.usecases.keywords

import me.nathanfallet.asonar.domain.models.apps.Store

/**
 * App Store scoring. Apple indexes words and does **not** stack a term repeated across fields: a term
 * in the title scores full, in the subtitle at half, and putting it in both doesn't add up — the
 * strongest field wins. Matches word-by-word, case/accent-folded, so "quoi manger" matches "Quoi
 * Manger ?". The wall/verdict math is inherited from [BaseOpportunityScorer].
 */
class AppStoreOpportunityScorer : BaseOpportunityScorer() {

    override val store = Store.APP_STORE

    override fun titleFactor(term: String, title: String, subtitle: String?): Double {
        val words = fold(term).split(' ').filter { it.length >= 2 }
        if (words.isEmpty()) return 0.0
        val t = fold(title)
        val ts = fold(title + " " + (subtitle ?: ""))
        return when {
            words.all { t.contains(it) } -> 1.0        // whole term in the title
            words.all { ts.contains(it) } -> 0.5       // only in title + subtitle together
            else -> 0.0
        }
    }
}
