package me.nathanfallet.asonar.infrastructure.di

import io.ktor.server.application.*
import me.nathanfallet.asonar.domain.repositories.AppsRepository
import me.nathanfallet.asonar.domain.repositories.KeywordsRepository
import me.nathanfallet.asonar.domain.repositories.PopularitySnapshotsRepository
import me.nathanfallet.asonar.domain.repositories.RankSnapshotsRepository
import me.nathanfallet.asonar.domain.repositories.TopAppSnapshotsRepository
import me.nathanfallet.asonar.domain.services.HealthService
import me.nathanfallet.asonar.infrastructure.database.DatabaseConfig
import me.nathanfallet.asonar.infrastructure.database.DatabaseFactory
import me.nathanfallet.asonar.infrastructure.database.H2DatabaseFactory
import me.nathanfallet.asonar.infrastructure.database.MySQLDatabaseFactory
import me.nathanfallet.asonar.infrastructure.database.TransactionManager
import me.nathanfallet.asonar.infrastructure.database.TransactionManagerImpl
import me.nathanfallet.asonar.infrastructure.database.repositories.AppsDatabaseRepository
import me.nathanfallet.asonar.infrastructure.database.repositories.KeywordsDatabaseRepository
import me.nathanfallet.asonar.infrastructure.database.repositories.PopularitySnapshotsDatabaseRepository
import me.nathanfallet.asonar.infrastructure.database.repositories.RankSnapshotsDatabaseRepository
import me.nathanfallet.asonar.infrastructure.database.repositories.TopAppSnapshotsDatabaseRepository
import me.nathanfallet.asonar.infrastructure.health.DatabaseHealthService
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for infrastructure-related dependencies: the database, and later the scrapers and the
 * repository implementations.
 */
val Application.infrastructureModule: Module
    get() = module {
        // Database
        single {
            DatabaseConfig(
                protocol = environment.config.property("database.protocol").getString(),
                host = environment.config.property("database.host").getString(),
                name = environment.config.property("database.name").getString(),
                user = environment.config.property("database.user").getString(),
                password = environment.config.property("database.password").getString(),
            )
        }
        single<DatabaseFactory> {
            val config = get<DatabaseConfig>()
            when (config.protocol) {
                "mysql" -> MySQLDatabaseFactory(config)
                "h2" -> H2DatabaseFactory(config)
                else -> throw IllegalArgumentException("Unsupported database protocol: ${config.protocol}")
            }
        }
        single<TransactionManager> { TransactionManagerImpl(get()) }

        single<HealthService> { DatabaseHealthService(get()) }

        // Repositories
        single<AppsRepository> { AppsDatabaseRepository(get()) }
        single<KeywordsRepository> { KeywordsDatabaseRepository(get()) }
        single<PopularitySnapshotsRepository> { PopularitySnapshotsDatabaseRepository(get()) }
        single<RankSnapshotsRepository> { RankSnapshotsDatabaseRepository(get()) }
        single<TopAppSnapshotsRepository> { TopAppSnapshotsDatabaseRepository(get()) }
    }
