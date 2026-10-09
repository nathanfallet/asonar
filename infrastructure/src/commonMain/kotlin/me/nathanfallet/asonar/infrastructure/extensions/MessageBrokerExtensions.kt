package me.nathanfallet.asonar.infrastructure.extensions

import dev.kourier.amqp.AMQPResponse
import dev.kourier.amqp.BuiltinExchangeType
import dev.kourier.amqp.Field
import dev.kourier.amqp.channel.AMQPChannel
import dev.kourier.amqp.channel.exchangeDeclare
import dev.kourier.amqp.channel.queueBind
import dev.kourier.amqp.channel.queueDeclare
import dev.kourier.amqp.states.DeclaredExchangeBuilder
import dev.kourier.amqp.states.DeclaredQueueBuilder
import dev.kourier.amqp.states.declaredExchange
import dev.kourier.amqp.states.declaredQueue
import io.ktor.callid.KtorCallIdContextElement
import io.ktor.http.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import me.nathanfallet.asonar.api.Serialization
import me.nathanfallet.asonar.infrastructure.messaging.MessageBroker
import me.nathanfallet.asonar.infrastructure.messaging.MessageHandlerResult
import org.slf4j.MDC
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.uuid.Uuid

/** Publishes a [T] to [exchange] with [routingKey], serialized to JSON. */
suspend inline fun <reified T> MessageBroker.publish(exchange: String, routingKey: String, message: T) {
    val jsonMessage = Serialization.json.encodeToString(message)
    publish(exchange, routingKey, jsonMessage)
}

/** Declares an exchange, plus its dead-letter (dlx, TTL'd) and dead exchanges/queues. */
suspend fun AMQPChannel.exchangeDeclareWithAdditionalResources(
    dlx: Boolean = true,
    dead: Boolean = true,
    block: DeclaredExchangeBuilder.() -> Unit,
) {
    val declaredExchange = declaredExchange(block)
    exchangeDeclare(declaredExchange)

    if (dlx) {
        exchangeDeclare {
            name = "${declaredExchange.name}-dlx"
            type = BuiltinExchangeType.FANOUT
            durable = true
        }
        queueDeclare {
            name = "${declaredExchange.name}-dlx"
            durable = true
            arguments = mapOf(
                "x-dead-letter-exchange" to Field.LongString(declaredExchange.name),
                "x-message-ttl" to Field.Int(5000),
            )
        }
        queueBind {
            queue = "${declaredExchange.name}-dlx"
            exchange = "${declaredExchange.name}-dlx"
            routingKey = "#"
        }
    }
    if (dead) {
        exchangeDeclare {
            name = "${declaredExchange.name}-dead"
            type = BuiltinExchangeType.FANOUT
            durable = true
        }
        queueDeclare {
            name = "${declaredExchange.name}-dead"
            durable = true
        }
        queueBind {
            queue = "${declaredExchange.name}-dead"
            exchange = "${declaredExchange.name}-dead"
            routingKey = "#"
        }
    }
}

/** Declares a queue, wiring its dead-letter exchange and optionally making it a quorum queue. */
suspend fun AMQPChannel.queueDeclareWithAdditionalResources(
    exchange: String,
    dlx: Boolean = true,
    quorum: Boolean = false,
    block: DeclaredQueueBuilder.() -> Unit,
) {
    val declaredQueue = declaredQueue(block)

    val dlxArguments =
        if (dlx) mapOf("x-dead-letter-exchange" to Field.LongString("$exchange-dlx"))
        else emptyMap()
    val quorumArguments =
        if (quorum && declaredQueue.durable && !declaredQueue.exclusive) {
            mapOf("x-queue-type" to Field.LongString("quorum"))
        } else emptyMap()

    queueDeclare(declaredQueue.copy(arguments = declaredQueue.arguments + dlxArguments + quorumArguments))
}

/** Context passed to [handleWithRetryAndDead]'s block: how many retries so far, and whether to retry/dead-letter. */
data class HandleWithRetryAndDeadContext(
    val retryCount: Long,
    val tryAgain: Boolean,
    val dead: Boolean,
)

/**
 * Runs [block]; on exception, rejects to the DLX to retry until [maxXDeathCount], then (if [dead])
 * moves the message to the dead-letter queue instead of looping forever.
 */
suspend inline fun AMQPChannel.handleWithRetryAndDead(
    delivery: AMQPResponse.Channel.Message.Delivery,
    maxXDeathCount: Int = 5,
    dead: Boolean = true,
    block: (HandleWithRetryAndDeadContext) -> MessageHandlerResult,
): MessageHandlerResult {
    val retryCount = delivery.message.properties.headers?.xDeathCount ?: 0L
    val tryAgain = retryCount < maxXDeathCount
    val context = HandleWithRetryAndDeadContext(retryCount, tryAgain, dead)
    return try {
        block(context)
    } catch (ce: CancellationException) {
        // Cancellation is not a handling failure, it is the consumer being torn down. Dead-lettering
        // it would drop a message nobody really attempted, and swallowing it would leave the
        // coroutine running past its own cancellation. The broker redelivers whatever is still
        // unacknowledged when the channel goes down.
        throw ce
    } catch (e: Throwable) {
        // This used to be `catch (e: Exception)`, which let every `Error` through — IllegalAccessError,
        // NoClassDefFoundError, StackOverflowError. Nothing below ran for those: no result, so the
        // caller neither acked nor nacked and the delivery stayed unsettled forever, holding a
        // prefetch slot for the life of the connection.
        val reason = e.toString()
        if (context.tryAgain) return MessageHandlerResult.Failure(reason, requeue = false)
        if (context.dead) sendToDeadLetterQueue(delivery, reason)
        MessageHandlerResult.Success
    }
}

/** Publishes [delivery] to the `<exchange>-dead` exchange, stamping the failure reason and time. */
suspend fun AMQPChannel.sendToDeadLetterQueue(
    delivery: AMQPResponse.Channel.Message.Delivery,
    reason: String,
) {
    val deadExchange = delivery.message.exchange + "-dead"
    val updatedHeaders = (delivery.message.properties.headers ?: emptyMap()).toMutableMap()
    updatedHeaders["x-failed-reason"] = Field.LongString(reason)
    updatedHeaders["x-failed-at"] = Field.Long(Clock.System.now().toEpochMilliseconds())

    val newProps = delivery.message.properties.copy(headers = updatedHeaders)
    basicPublish(
        body = delivery.message.body,
        exchange = deadExchange,
        routingKey = delivery.message.routingKey,
        properties = newProps,
    )
}

/** The retry count RabbitMQ tracks in the `x-death` header. */
val Map<String, Field>.xDeathCount: Long?
    get() {
        val xDeathArray = get("x-death") as? Field.Array
        val xDeath = xDeathArray?.value?.firstOrNull() as? Field.Table
        return (xDeath?.value?.get("count") as? Field.Long)?.value
    }

/** The MDC key logback prints (`%X{call-id}`), the same one `configureMonitoring` fills for HTTP calls. */
const val CALL_ID_MDC = "call-id"

/**
 * The current request id as an AMQP header, so a fetch queued by `POST /api/keywords` logs under the
 * id of the request that queued it. Empty outside a request (the id lives in the coroutine context,
 * put there by the `CallId` plugin).
 */
suspend fun mapOfRequestId(): Map<String, Field> =
    currentCoroutineContext()[KtorCallIdContextElement]
        ?.let { mapOf(HttpHeaders.XRequestId to Field.LongString(it.callId)) }
        ?: emptyMap()

/**
 * Runs [block] under the request id carried by this delivery, both in the coroutine context (so
 * whatever it publishes carries the id further) and in the MDC (so its logs print it). A message with
 * no id, published outside any request, gets a fresh one: the logs of one fetch still group together.
 */
suspend fun <T> AMQPResponse.Channel.Message.Delivery.withRequestId(block: suspend () -> T): T {
    val callId = (message.properties.headers?.get(HttpHeaders.XRequestId) as? Field.LongString)?.value
        ?: Uuid.random().toString()
    val mdc = (MDC.getCopyOfContextMap() ?: emptyMap()) + (CALL_ID_MDC to callId)
    return withContext(KtorCallIdContextElement(callId) + MDCContext(mdc)) { block() }
}

