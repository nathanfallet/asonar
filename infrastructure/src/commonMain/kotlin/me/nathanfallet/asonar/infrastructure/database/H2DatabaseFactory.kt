package me.nathanfallet.asonar.infrastructure.database

import org.jetbrains.exposed.v1.jdbc.Database

/**
 * [DatabaseFactory] for H2.
 *
 * With a blank [DatabaseConfig.directory] it runs fully in-memory (what the tests use). With a
 * directory set it opens a file-backed database there, so a local run keeps its keyword history
 * across restarts — the `data/` dir is gitignored. The directory is normalized to a form H2 accepts
 * (it rejects a bare relative path, so `data` becomes `./data`).
 */
class H2DatabaseFactory(
    private val config: DatabaseConfig,
) : DatabaseFactory {

    private val db: Database by lazy {
        val url = if (config.directory.isBlank()) {
            "jdbc:h2:mem:${config.name};DB_CLOSE_DELAY=-1;MODE=MySQL;"
        } else {
            "jdbc:h2:file:${normalizeDirectory(config.directory)}/${config.name};MODE=MySQL;"
        }
        Database.connect(url, "org.h2.Driver")
    }

    override fun getDatabase(): Database = db

    // A file-backed H2 is up for as long as the process is, same as in-memory.
    override fun isHealthy(): Boolean = true

    private fun normalizeDirectory(directory: String): String {
        val trimmed = directory.trimEnd('/')
        return if (trimmed.startsWith("/") || trimmed.startsWith("~") || trimmed.startsWith("./")) {
            trimmed
        } else {
            "./$trimmed"
        }
    }

}
