package me.nathanfallet.asonar.infrastructure.messaging

import dev.kourier.amqp.channel.AMQPChannel

/** Owns the AMQP connection and channel, and declares the topology. */
interface RabbitMQFactory {

    /** Connects and declares the exchange, queue and bindings. */
    suspend fun initialize()

    /** The channel to publish/consume on. */
    fun getChannel(): AMQPChannel

}
