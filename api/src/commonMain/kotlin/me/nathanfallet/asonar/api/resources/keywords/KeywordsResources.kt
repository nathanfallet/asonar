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
    )
}
