package me.nathanfallet.asonar.infrastructure.messaging

/** The outcome of handling a message. */
sealed class MessageHandlerResult {

    /** Handled successfully — the message is acked. */
    object Success : MessageHandlerResult()

    /** Handling failed — the message is nacked, and requeued if [requeue]. */
    data class Failure(
        val reason: String,
        val requeue: Boolean = false,
    ) : MessageHandlerResult()

}
