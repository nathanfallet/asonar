package me.nathanfallet.asonar.infrastructure.messaging

import dev.kourier.amqp.BuiltinExchangeType
import dev.kourier.amqp.channel.AMQPChannel
import dev.kourier.amqp.channel.queueBind
import dev.kourier.amqp.connection.AMQPConnection
import dev.kourier.amqp.robust.createRobustAMQPConnection
import kotlinx.coroutines.CoroutineScope
import me.nathanfallet.asonar.infrastructure.extensions.exchangeDeclareWithAdditionalResources
import me.nathanfallet.asonar.infrastructure.extensions.queueDeclareWithAdditionalResources

/**
 * [RabbitMQFactory] backed by kourier's robust (auto-reconnecting) connection. Declares one durable
 * topic exchange + one durable quorum queue, with dead-letter + dead exchanges for retry/DLQ.
 *
 * `basicQos(1u)` is deliberate: exactly one unacked delivery at a time, so fetches run strictly
 * one-by-one — polite crawling that won't get us rate-limited by the stores.
 */
class RabbitMQFactoryImpl(
    private val coroutineScope: CoroutineScope,
    private val host: String,
    private val port: Int,
    private val user: String,
    private val password: String,
) : RabbitMQFactory {

    private lateinit var amqpConnection: AMQPConnection
    private lateinit var amqpChannel: AMQPChannel

    override suspend fun initialize() {
        amqpConnection = createRobustAMQPConnection(coroutineScope) {
            server {
                host = this@RabbitMQFactoryImpl.host
                port = this@RabbitMQFactoryImpl.port
                user = this@RabbitMQFactoryImpl.user
                password = this@RabbitMQFactoryImpl.password
            }
        }

        amqpChannel = amqpConnection.openChannel().apply {
            basicQos(1u)

            exchangeDeclareWithAdditionalResources(dlx = true, dead = true) {
                name = Messaging.EXCHANGE
                type = BuiltinExchangeType.TOPIC
                durable = true
            }
            queueDeclareWithAdditionalResources(exchange = Messaging.EXCHANGE, dlx = true, quorum = true) {
                name = Messaging.QUEUE
                durable = true
            }
            queueBind {
                queue = Messaging.QUEUE
                exchange = Messaging.EXCHANGE
                routingKey = "#"
            }
        }
    }

    override fun getChannel(): AMQPChannel = amqpChannel

}
