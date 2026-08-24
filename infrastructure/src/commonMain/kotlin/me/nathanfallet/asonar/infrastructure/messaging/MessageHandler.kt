package me.nathanfallet.asonar.infrastructure.messaging

import dev.kourier.amqp.AMQPResponse
import dev.kourier.amqp.channel.AMQPChannel

/** Handles an incoming message delivery. */
interface MessageHandler {

    suspend operator fun invoke(
        channel: AMQPChannel,
        delivery: AMQPResponse.Channel.Message.Delivery,
    ): MessageHandlerResult

}
