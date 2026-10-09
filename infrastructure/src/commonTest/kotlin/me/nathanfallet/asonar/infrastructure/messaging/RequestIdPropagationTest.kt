package me.nathanfallet.asonar.infrastructure.messaging

import dev.kourier.amqp.AMQPMessage
import dev.kourier.amqp.AMQPResponse
import dev.kourier.amqp.Field
import dev.kourier.amqp.Properties
import dev.kourier.amqp.channel.AMQPChannel
import io.ktor.callid.KtorCallIdContextElement
import io.ktor.http.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import me.nathanfallet.asonar.infrastructure.extensions.CALL_ID_MDC
import org.slf4j.MDC
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A fetch queued by an HTTP request must log under that request's id: the id leaves with the
 * published message, and comes back into the MDC and the coroutine context of whoever consumes it.
 */
class RequestIdPropagationTest {

    private val channel = mockk<AMQPChannel>(relaxed = true)
    private val factory = mockk<RabbitMQFactory> { every { getChannel() } returns channel }
    private val broker = RabbitMQMessageBroker(factory)

    private fun delivery(headers: Map<String, Field>) = AMQPResponse.Channel.Message.Delivery(
        message = AMQPMessage(
            exchange = Messaging.EXCHANGE,
            routingKey = Messaging.ROUTING_KEYWORD_FETCH,
            deliveryTag = 7uL,
            properties = Properties(headers = headers),
            redelivered = false,
            body = "{}".encodeToByteArray(),
        ),
        consumerTag = "test",
    )

    /** What the handler saw: the MDC id and the coroutine-context id. */
    private suspend fun consume(delivery: AMQPResponse.Channel.Message.Delivery): Pair<String?, String?> {
        val onDelivery = slot<suspend (AMQPResponse.Channel.Message.Delivery) -> Unit>()
        coEvery {
            channel.basicConsume(any(), any(), any(), any(), any(), capture(onDelivery), any())
        } returns AMQPResponse.Channel.Basic.ConsumeOk("consumer-tag")
        var seen: Pair<String?, String?> = null to null
        broker.startConsuming(Messaging.QUEUE, object : MessageHandler {
            override suspend fun invoke(
                channel: AMQPChannel,
                delivery: AMQPResponse.Channel.Message.Delivery,
            ): MessageHandlerResult {
                seen = MDC.get(CALL_ID_MDC) to currentCoroutineContext()[KtorCallIdContextElement]?.callId
                return MessageHandlerResult.Success
            }
        })
        onDelivery.captured(delivery)
        return seen
    }

    @Test
    fun `a message published during a request carries its id`() = runTest {
        val properties = slot<Properties>()
        coEvery { channel.basicPublish(any(), any(), any(), any(), any(), capture(properties)) } returns mockk()

        withContext(KtorCallIdContextElement("req-1")) {
            broker.publish(Messaging.EXCHANGE, Messaging.ROUTING_KEYWORD_FETCH, "{}", null)
        }

        assertEquals("req-1", (properties.captured.headers?.get(HttpHeaders.XRequestId) as? Field.LongString)?.value)
    }

    @Test
    fun `a message published outside any request carries no id`() = runTest {
        val properties = slot<Properties>()
        coEvery { channel.basicPublish(any(), any(), any(), any(), any(), capture(properties)) } returns mockk()

        broker.publish(Messaging.EXCHANGE, Messaging.ROUTING_KEYWORD_FETCH, "{}", mapOf("x-retry" to Field.LongString("1")))

        assertEquals(setOf("x-retry"), properties.captured.headers?.keys)
    }

    @Test
    fun `the consumer runs under the id the message carries`() = runTest {
        val (mdc, context) = consume(delivery(mapOf(HttpHeaders.XRequestId to Field.LongString("req-1"))))

        assertEquals("req-1", mdc)
        // Also in the coroutine context, so whatever the handler publishes carries it further.
        assertEquals("req-1", context)
    }

    @Test
    fun `a message without an id still gets one for its logs`() = runTest {
        val (mdc, context) = consume(delivery(emptyMap()))

        assertNotNull(mdc)
        assertTrue(mdc.isNotBlank())
        assertEquals(mdc, context)
    }

}
