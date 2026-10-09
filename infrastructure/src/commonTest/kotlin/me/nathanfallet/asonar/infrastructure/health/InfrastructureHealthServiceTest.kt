package me.nathanfallet.asonar.infrastructure.health

import dev.kourier.amqp.channel.AMQPChannel
import dev.kourier.amqp.connection.ConnectionState
import io.mockk.every
import io.mockk.mockk
import me.nathanfallet.asonar.infrastructure.database.DatabaseFactory
import me.nathanfallet.asonar.infrastructure.messaging.RabbitMQFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class InfrastructureHealthServiceTest {

    private val database = mockk<DatabaseFactory> { every { isHealthy() } returns true }

    private fun broker(state: ConnectionState) = mockk<RabbitMQFactory> {
        every { getChannel() } returns mockk<AMQPChannel> { every { this@mockk.state } returns state }
    }

    @Test
    fun `everything up`() {
        val service = InfrastructureHealthService(database, broker(ConnectionState.OPEN), messagingEnabled = true)

        assertEquals(mapOf("database" to true, "messaging" to true), service.check())
    }

    @Test
    fun `a closed broker channel is reported down`() {
        val service = InfrastructureHealthService(database, broker(ConnectionState.CLOSED), messagingEnabled = true)

        assertEquals(false, service.check()["messaging"])
    }

    @Test
    fun `a broker never initialized is reported down instead of throwing`() {
        val broker = mockk<RabbitMQFactory> {
            every { getChannel() } throws UninitializedPropertyAccessException("amqpChannel")
        }
        val service = InfrastructureHealthService(database, broker, messagingEnabled = true)

        assertEquals(false, service.check()["messaging"])
    }

    @Test
    fun `messaging is not checked where the broker is never started`() {
        val broker = mockk<RabbitMQFactory> { every { getChannel() } throws IllegalStateException() }
        val service = InfrastructureHealthService(database, broker, messagingEnabled = false)

        assertEquals(true, service.check()["messaging"])
    }

}
