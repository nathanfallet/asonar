package me.nathanfallet.asonar.api.resources.keywords

import io.ktor.resources.*
import kotlinx.serialization.Serializable

/**
 * An app's discovered keyword candidates — terms proposed for tracking, awaiting a call.
 *
 * `GET` lists them ([status] filters the review state, comma-separated, default the pending ones;
 * [minPopularity] hides the small fry), `POST` runs a discovery pass.
 */
@Serializable
@Resource("/api/keyword-candidates")
class KeywordCandidatesApi(
    val appId: Long,
    val status: String? = null,
    val minPopularity: Int? = null,
) {

    /** Acts on reviewed candidates: accept them (track the terms) or dismiss them for good. */
    @Serializable
    @Resource("review")
    class Review(val parent: KeywordCandidatesApi)

}
