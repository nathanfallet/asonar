package me.nathanfallet.asonar.api.resources.apps

import io.ktor.resources.*
import kotlinx.serialization.Serializable

/** The app endpoints of the API. */
@Serializable
@Resource("/api/apps")
class AppsApi {

    /** A single app by its identifier. */
    @Resource("{id}")
    class Id(
        val parent: AppsApi = AppsApi(),
        val id: Long,
    )
}
