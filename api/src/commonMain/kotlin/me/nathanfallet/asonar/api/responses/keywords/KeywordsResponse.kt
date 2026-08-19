package me.nathanfallet.asonar.api.responses.keywords

import kotlinx.serialization.Serializable

/** A list of tracked keywords. */
@Serializable
data class KeywordsResponse(
    val keywords: List<KeywordResponse>,
)
