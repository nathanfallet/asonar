package me.nathanfallet.asonar.infrastructure.messaging

import dev.kourier.amqp.Field

/** A message broker that can publish messages and start consumers. */
interface MessageBroker {

    /** Publishes a message to [exchange] with [routingKey]. */
    suspend fun publish(exchange: String, routingKey: String, message: String, headers: Map<String, Field>? = null)

    /** Starts consuming [queue], dispatching each delivery to [handler]. */
    suspend fun startConsuming(queue: String, handler: MessageHandler)

}
