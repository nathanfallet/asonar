package me.nathanfallet.asonar.infrastructure.health

import dev.kourier.amqp.connection.ConnectionState
import me.nathanfallet.asonar.domain.services.HealthService
import me.nathanfallet.asonar.infrastructure.database.DatabaseFactory
import me.nathanfallet.asonar.infrastructure.messaging.RabbitMQFactory

/**
 * [HealthService] over the database and RabbitMQ. A dead broker used to go unnoticed: the API kept
 * answering while every fetch queued behind it waited forever.
 *
 * [messagingEnabled] is false where the broker is deliberately never started (the `test`
 * environment), so its absence is not reported as an outage there.
 */
class InfrastructureHealthService(
    private val databaseFactory: DatabaseFactory,
    private val rabbitMQFactory: RabbitMQFactory,
    private val messagingEnabled: Boolean,
) : HealthService {

    override fun check(): Map<String, Boolean> = mapOf(
        "database" to safely { databaseFactory.isHealthy() },
        "messaging" to (!messagingEnabled || safely { rabbitMQFactory.getChannel().state == ConnectionState.OPEN }),
    )

    // A probe answers, it never throws: before `initialize()` there is no channel to ask at all.
    private fun safely(check: () -> Boolean): Boolean =
        try {
            check()
        } catch (e: Exception) {
            false
        }

}
