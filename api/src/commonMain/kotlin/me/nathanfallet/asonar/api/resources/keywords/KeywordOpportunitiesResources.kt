package me.nathanfallet.asonar.api.resources.keywords

import io.ktor.resources.*
import kotlinx.serialization.Serializable

/** An app's keyword opportunities (recommendations), scored from the tracked keywords on its store. */
@Serializable
@Resource("/api/keyword-opportunities")
class KeywordOpportunitiesApi(
    val appId: Long,
)
