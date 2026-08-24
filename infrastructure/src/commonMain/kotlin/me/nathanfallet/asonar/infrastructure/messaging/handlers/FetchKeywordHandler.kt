package me.nathanfallet.asonar.infrastructure.messaging.handlers

import dev.kourier.amqp.AMQPResponse
import dev.kourier.amqp.channel.AMQPChannel
import kotlinx.serialization.decodeFromString
import me.nathanfallet.asonar.api.Serialization
import me.nathanfallet.asonar.infrastructure.extensions.handleWithRetryAndDead
import me.nathanfallet.asonar.infrastructure.messaging.MessageHandler
import me.nathanfallet.asonar.infrastructure.messaging.MessageHandlerResult
import me.nathanfallet.asonar.infrastructure.messaging.Messaging
import me.nathanfallet.asonar.infrastructure.messaging.messages.FetchKeywordMessage
import org.slf4j.LoggerFactory

/**
 * Consumes fetch requests. For now it only logs the intent — the actual scraping (kdriver → the
 * stores) plus [me.nathanfallet.asonar.domain.usecases.runs.RecordKeywordRunUseCase] is the next
 * piece, and plugs in right here.
 */
class FetchKeywordHandler : MessageHandler {

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
                logger.info("[fetch] would fetch keyword ${message.keywordId} — scraper not yet implemented")
                MessageHandlerResult.Success
            }

            else -> MessageHandlerResult.Success // ignore unknown routing keys
        }
    }

}
