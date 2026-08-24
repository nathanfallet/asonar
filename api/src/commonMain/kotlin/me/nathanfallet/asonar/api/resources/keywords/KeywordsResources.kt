package me.nathanfallet.asonar.api.resources.keywords

import io.ktor.resources.*
import kotlinx.serialization.Serializable

/** The keyword endpoints of the API — the contract shared by the server routes and the client. */
@Serializable
@Resource("/api/keywords")
class KeywordsApi {

    /** A single keyword by its identifier. */
    @Resource("{id}")
    class Id(
        val parent: KeywordsApi = KeywordsApi(),
        val id: Long,
    ) {

        /** The keyword's popularity history. */
        @Resource("popularity")
        class Popularity(val parent: Id)

        /** The keyword's most recent top-of-results. */
        @Resource("top-apps")
        class TopApps(val parent: Id)

        /** The rank history of one of our apps on the keyword. */
        @Resource("ranks/{appId}")
        class Ranks(val parent: Id, val appId: Long)

        /** POST here to queue a fetch of the keyword's data. */
        @Resource("refresh")
        class Refresh(val parent: Id)
    }
}
