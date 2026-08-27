package me.nathanfallet.asonar.domain.repositories

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshot
import me.nathanfallet.asonar.domain.models.snapshots.PopularitySnapshotPayload

/** Records and reads the popularity history of the keywords. */
interface PopularitySnapshotsRepository {

    /** Records a popularity reading. */
    suspend fun create(payload: PopularitySnapshotPayload): PopularitySnapshot

    /** Lists the popularity history of a keyword, newest first. */
    suspend fun listForKeyword(keywordId: Long, pagination: Pagination): List<PopularitySnapshot>

    /** Reads the most recent popularity of a keyword. */
    suspend fun getLatestForKeyword(keywordId: Long): PopularitySnapshot?

    /** The latest popularity snapshot of every keyword, in one read (keyed by keyword id). */
    suspend fun latestByKeyword(): Map<Long, PopularitySnapshot>

}
