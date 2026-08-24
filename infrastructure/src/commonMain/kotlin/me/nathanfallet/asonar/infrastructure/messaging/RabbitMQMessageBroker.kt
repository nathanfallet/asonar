package me.nathanfallet.asonar.infrastructure.messaging

import dev.kourier.amqp.Field
import dev.kourier.amqp.properties

class RabbitMQMessageBroker(
    private val rabbitMQFactory: RabbitMQFactory,
) : MessageBroker {

    override suspend fun publish(exchange: String, routingKey: String, message: String, headers: Map<String, Field>?) {
        rabbitMQFactory.getChannel().basicPublish(
            body = message.toByteArray(),
            exchange = exchange,
            routingKey = routingKey,
            properties = properties {
                deliveryMode = 2u // persistent
                this@properties.headers = headers ?: emptyMap()
            },
        )
    }

    override suspend fun startConsuming(queue: String, handler: MessageHandler) {
        val channel = rabbitMQFactory.getChannel()
        channel.basicConsume(
            queue = queue,
            noAck = false,
            onDelivery = { delivery ->
                when (val result = handler(channel, delivery)) {
                    is MessageHandlerResult.Success -> channel.basicAck(delivery.message.deliveryTag)
                    is MessageHandlerResult.Failure -> channel.basicNack(
                        delivery.message.deliveryTag,
                        requeue = result.requeue,
                    )
                }
            },
        )
    }

}
