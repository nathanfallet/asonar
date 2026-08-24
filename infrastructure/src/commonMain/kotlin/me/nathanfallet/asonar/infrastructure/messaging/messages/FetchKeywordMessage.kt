package me.nathanfallet.asonar.infrastructure.messaging.messages

import kotlinx.serialization.Serializable

/** Queue payload: "fetch this keyword's data". Internal to the fetch pipeline, not an API type. */
@Serializable
data class FetchKeywordMessage(
    val keywordId: Long,
)
