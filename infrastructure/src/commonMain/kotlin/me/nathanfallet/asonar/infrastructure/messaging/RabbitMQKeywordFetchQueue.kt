package me.nathanfallet.asonar.infrastructure.messaging

import me.nathanfallet.asonar.domain.services.KeywordFetchQueue
import me.nathanfallet.asonar.infrastructure.extensions.publish
import me.nathanfallet.asonar.infrastructure.messaging.messages.FetchKeywordMessage

/** [KeywordFetchQueue] that publishes fetch requests onto RabbitMQ. */
class RabbitMQKeywordFetchQueue(
    private val messageBroker: MessageBroker,
) : KeywordFetchQueue {

    override suspend fun enqueueFetch(keywordId: Long) {
        messageBroker.publish(
            exchange = Messaging.EXCHANGE,
            routingKey = Messaging.ROUTING_KEYWORD_FETCH,
            message = FetchKeywordMessage(keywordId),
        )
    }

}
