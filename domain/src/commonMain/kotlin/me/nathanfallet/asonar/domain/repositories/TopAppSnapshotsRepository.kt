package me.nathanfallet.asonar.domain.repositories

import me.nathanfallet.asonar.domain.models.snapshots.TopAppSnapshot
import me.nathanfallet.asonar.domain.models.snapshots.TopAppSnapshotPayload

/** Records and reads the top-of-results (competitors) for the keywords. */
interface TopAppSnapshotsRepository {

    /** Records one row of a keyword's top-of-results. */
    suspend fun create(payload: TopAppSnapshotPayload): TopAppSnapshot

    /**
     * Reads the most recent top-of-results of a keyword, ordered by position — i.e. the rows of the
     * latest observation.
     */
    suspend fun listLatestForKeyword(keywordId: Long): List<TopAppSnapshot>

}
