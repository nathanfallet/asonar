package me.nathanfallet.asonar.infrastructure.messaging

/** Topology + routing constants for the asonar message broker. */
object Messaging {
    const val EXCHANGE = "asonar"
    const val QUEUE = "asonar-queue"

    /** Routing key for "please fetch this keyword's data". */
    const val ROUTING_KEYWORD_FETCH = "keywords.fetch"
}
