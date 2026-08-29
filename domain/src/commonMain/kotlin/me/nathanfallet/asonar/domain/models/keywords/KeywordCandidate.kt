package me.nathanfallet.asonar.domain.models.keywords

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * A term a discovery source proposed for an app, *before* we commit to tracking it. Candidates are
 * persisted rather than recomputed on the fly for one reason: **dismissing has to stick.** Discovery
 * returns a lot of generic noise, and a candidate rejected once must not come back on the next run.
 *
 * Identity is (app, term, country) — the store comes from the app. A candidate is scoped to the app
 * it was discovered *for*: the same term can be a lead for one app and noise for another, so
 * dismissing it here never hides it there.
 *
 * [sources] is a set because the same term often surfaces from several angles (Apple suggested it
 * *and* a competitor puts it in their subtitle) — that agreement is itself a relevance signal, so
 * re-discovery merges into the row instead of replacing it. [popularity] is filled only when the
 * source knows it: Apple Search Ads suggestions come *with* the search index, which is exactly why
 * they beat guessing — the terms we invent overwhelmingly floor at 5 (see docs/ROADMAP.md).
 */
@Serializable
data class KeywordCandidate(
    val id: Long,
    val appId: Long,
    val term: String,
    val country: String, // ISO 3166-1 alpha-2, e.g. "FR"
    val sources: Set<CandidateSource>,
    val detail: String? = null, // where it came from, for a human: seed term, competitor id…
    val popularity: Int? = null, // 0–100, when the source provides it
    val status: CandidateStatus,
    val discoveredAt: Instant,
    val updatedAt: Instant,
)

/** What one discovery run proposes. Merged into any existing candidate for (app, term, country). */
@Serializable
data class KeywordCandidatePayload(
    val appId: Long,
    val term: String,
    val country: String,
    val source: CandidateSource,
    val detail: String? = null,
    val popularity: Int? = null,
)

/** Where a candidate came from. Its name is what gets stored in the database. */
@Serializable
enum class CandidateSource {
    /** Apple Search Ads keyword recommendations — the only source that carries real search volume. */
    ASA,

    /** Terms a competitor indexes: their title, subtitle and description. */
    COMPETITOR_METADATA,

    /** Words that recur in reviews — ours and our competitors'. */
    REVIEWS,
}

/** Where a candidate sits in the review loop. */
@Serializable
enum class CandidateStatus {
    /** Proposed, awaiting a call. */
    NEW,

    /** Accepted — a tracked keyword now exists for it. */
    ADDED,

    /** Rejected. Re-discovery must never resurrect it; that is the point of persisting candidates. */
    DISMISSED,
}
