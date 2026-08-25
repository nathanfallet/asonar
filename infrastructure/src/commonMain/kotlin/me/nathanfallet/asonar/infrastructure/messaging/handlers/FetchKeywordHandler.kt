package me.nathanfallet.asonar.infrastructure.messaging.handlers

import dev.kourier.amqp.AMQPResponse
import dev.kourier.amqp.channel.AMQPChannel
import me.nathanfallet.asonar.api.Serialization
import me.nathanfallet.asonar.domain.usecases.keywords.FetchKeywordUseCase
import me.nathanfallet.asonar.infrastructure.extensions.handleWithRetryAndDead
import me.nathanfallet.asonar.infrastructure.messaging.MessageHandler
import me.nathanfallet.asonar.infrastructure.messaging.MessageHandlerResult
import me.nathanfallet.asonar.infrastructure.messaging.Messaging
import me.nathanfallet.asonar.infrastructure.messaging.messages.FetchKeywordMessage
import org.slf4j.LoggerFactory

/**
 * Consumes fetch requests and runs [FetchKeywordUseCase], which pulls the keyword's data from the
 * store sources and records it. Retries via the DLX up to 5 times, then dead-letters.
 */
class FetchKeywordHandler(
    private val fetchKeywordUseCase: FetchKeywordUseCase,
) : MessageHandler {

    private val logger = LoggerFactory.getLogger(FetchKeywordHandler::class.java)

    override suspend fun invoke(
        channel: AMQPChannel,
        delivery: AMQPResponse.Channel.Message.Delivery,
    ): MessageHandlerResult = channel.handleWithRetryAndDead(delivery, maxXDeathCount = 5, dead = true) {
        when (delivery.message.routingKey) {
            Messaging.ROUTING_KEYWORD_FETCH -> {
                val message = Serialization.json.decodeFromString<FetchKeywordMessage>(
                    delivery.message.body.decodeToString(),
                )
                logger.info("[fetch] fetching keyword ${message.keywordId}")
                fetchKeywordUseCase(message.keywordId)
                MessageHandlerResult.Success
            }

            else -> MessageHandlerResult.Success // ignore unknown routing keys
        }
    }

}
