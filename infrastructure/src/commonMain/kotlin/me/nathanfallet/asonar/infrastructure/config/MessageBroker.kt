package me.nathanfallet.asonar.infrastructure.config

import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import me.nathanfallet.asonar.infrastructure.messaging.MessageBroker
import me.nathanfallet.asonar.infrastructure.messaging.Messaging
import me.nathanfallet.asonar.infrastructure.messaging.RabbitMQFactory
import me.nathanfallet.asonar.infrastructure.messaging.handlers.FetchKeywordHandler
import org.koin.ktor.ext.get
import org.koin.ktor.ext.inject

/**
 * Connects to RabbitMQ and starts the in-process consumer. Same JVM as the HTTP server — a single
 * `docker compose up` + `./gradlew :app:run` runs both the API and the fetch worker.
 */
fun Application.configureMessageBroker() = runBlocking {
    if (environment.config.property("ktor.environment").getString() == "test") return@runBlocking
    val rabbitMQFactory by inject<RabbitMQFactory>()
    rabbitMQFactory.initialize()
    val messageBroker by inject<MessageBroker>()
    messageBroker.startConsuming(Messaging.QUEUE, get<FetchKeywordHandler>())
}
