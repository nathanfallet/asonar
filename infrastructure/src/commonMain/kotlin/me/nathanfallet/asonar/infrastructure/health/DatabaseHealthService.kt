package me.nathanfallet.asonar.infrastructure.health

import me.nathanfallet.asonar.domain.services.HealthService
import me.nathanfallet.asonar.infrastructure.database.DatabaseFactory

/** [HealthService] that reports healthy when the database connection is up. */
class DatabaseHealthService(
    private val databaseFactory: DatabaseFactory,
) : HealthService {

    override fun isHealthy(): Boolean = databaseFactory.isHealthy()

}
