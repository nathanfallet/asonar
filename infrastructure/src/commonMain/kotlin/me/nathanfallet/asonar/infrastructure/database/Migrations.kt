package me.nathanfallet.asonar.infrastructure.database

import org.flywaydb.core.api.configuration.FluentConfiguration

/**
 * Brings the schema up to date with the versioned SQL scripts in `db/migration` (Flyway). The
 * scripts are the source of truth for the schema; the Exposed tables only describe it to the code.
 * Write a new one with `./gradlew :infrastructure:generateMigrations`, then read it before keeping it.
 */
internal object Migrations {

    private const val LOCATION = "classpath:db/migration"

    /**
     * The version of `V1__baseline.sql`: the schema as it stood when the tables were still created by
     * `SchemaUtils.create`. A database from that era has tables but no Flyway history, so Flyway
     * records it at this version instead of replaying the baseline on top of it.
     */
    private const val BASELINE_VERSION = "1"

    fun migrate(configuration: FluentConfiguration) {
        configuration
            .locations(LOCATION)
            .baselineOnMigrate(true)
            .baselineVersion(BASELINE_VERSION)
            .load()
            .migrate()
    }

}
