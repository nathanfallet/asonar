package me.nathanfallet.asonar.api.resources.apps

import io.ktor.resources.*
import kotlinx.serialization.Serializable

/** One of our apps' ranking coverage across every keyword tracked on its store. */
@Serializable
@Resource("/api/app-coverage")
class AppCoverageApi(
    val appId: Long,
)
