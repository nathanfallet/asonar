package me.nathanfallet.asonar.domain.repositories

import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.snapshots.RankSnapshot
import me.nathanfallet.asonar.domain.models.snapshots.RankSnapshotPayload

/** Records and reads the rank history of our apps on the keywords. */
interface RankSnapshotsRepository {

    /** Records a rank reading. */
    suspend fun create(payload: RankSnapshotPayload): RankSnapshot

    /** Records many readings in a single batch insert (one statement, one transaction). */
    suspend fun createAll(payloads: List<RankSnapshotPayload>): List<RankSnapshot>

    /** Lists the rank history of an app on a keyword, newest first. */
    suspend fun listForKeywordAndApp(
        keywordId: Long,
        appId: Long,
        pagination: Pagination,
    ): List<RankSnapshot>

    /** Reads the most recent rank of an app on a keyword. */
    suspend fun getLatestForKeywordAndApp(keywordId: Long, appId: Long): RankSnapshot?

    /** The latest rank of [appId] on every keyword, in one read (keyed by keyword id). */
    suspend fun latestByKeywordForApp(appId: Long): Map<Long, RankSnapshot>

    /** The full rank history of [appId] on every keyword, newest first, in one read (keyed by keyword id). */
    suspend fun historyByKeywordForApp(appId: Long): Map<Long, List<RankSnapshot>>

}
