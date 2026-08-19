package me.nathanfallet.asonar.infrastructure.database

import org.jetbrains.exposed.v1.jdbc.Database

/**
 * [DatabaseFactory] for H2.
 *
 * With an empty [DatabaseConfig.host] it runs fully in-memory (what the tests use). With a host set
 * to a directory path it opens a file-backed database there, so a local run keeps its keyword
 * history across restarts — the `data/` dir is gitignored.
 */
class H2DatabaseFactory(
    private val config: DatabaseConfig,
) : DatabaseFactory {

    private val db: Database by lazy {
        val url = if (config.host.isEmpty()) {
            "jdbc:h2:mem:${config.name};DB_CLOSE_DELAY=-1;MODE=MySQL;"
        } else {
            "jdbc:h2:file:${config.host.trimEnd('/')}/${config.name};MODE=MySQL;"
        }
        Database.connect(url, "org.h2.Driver")
    }

    override fun getDatabase(): Database = db

    // A file-backed H2 is up for as long as the process is, same as in-memory.
    override fun isHealthy(): Boolean = true

}
