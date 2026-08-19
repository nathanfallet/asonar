package me.nathanfallet.asonar.client.api.keywords

import me.nathanfallet.asonar.api.requests.keywords.TrackKeywordRequest
import me.nathanfallet.asonar.api.responses.keywords.KeywordDetailResponse
import me.nathanfallet.asonar.api.responses.keywords.KeywordResponse
import me.nathanfallet.asonar.api.responses.keywords.KeywordsResponse
import me.nathanfallet.asonar.api.responses.snapshots.PopularitySnapshotsResponse
import me.nathanfallet.asonar.api.responses.snapshots.RankSnapshotsResponse
import me.nathanfallet.asonar.api.responses.snapshots.TopAppSnapshotsResponse

/** Client for the keyword endpoints. */
interface KeywordsApiClient {

    /** Lists the tracked keywords with their latest popularity. */
    suspend fun getAll(): KeywordsResponse

    /** Reads one keyword's full detail. */
    suspend fun get(id: Long): KeywordDetailResponse

    /** Starts tracking a keyword (idempotent). */
    suspend fun track(request: TrackKeywordRequest): KeywordResponse

    /** Stops tracking a keyword. */
    suspend fun delete(id: Long)

    /** Reads a keyword's popularity history. */
    suspend fun popularityHistory(id: Long): PopularitySnapshotsResponse

    /** Reads a keyword's most recent top-of-results. */
    suspend fun topApps(id: Long): TopAppSnapshotsResponse

    /** Reads the rank history of one of our apps on a keyword. */
    suspend fun ranks(id: Long, appId: Long): RankSnapshotsResponse

}
