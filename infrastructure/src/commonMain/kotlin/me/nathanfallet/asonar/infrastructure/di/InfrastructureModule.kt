package me.nathanfallet.asonar.infrastructure.di

import io.ktor.server.application.*
import me.nathanfallet.asonar.domain.repositories.AppsRepository
import me.nathanfallet.asonar.domain.repositories.KeywordsRepository
import me.nathanfallet.asonar.domain.repositories.PopularitySnapshotsRepository
import me.nathanfallet.asonar.domain.repositories.RankSnapshotsRepository
import me.nathanfallet.asonar.domain.repositories.TopAppSnapshotsRepository
import me.nathanfallet.asonar.domain.services.HealthService
import me.nathanfallet.asonar.domain.services.KeywordFetchQueue
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
import me.nathanfallet.asonar.infrastructure.messaging.MessageBroker
import me.nathanfallet.asonar.infrastructure.messaging.RabbitMQFactory
import me.nathanfallet.asonar.infrastructure.messaging.RabbitMQFactoryImpl
import me.nathanfallet.asonar.infrastructure.messaging.RabbitMQKeywordFetchQueue
import me.nathanfallet.asonar.infrastructure.messaging.RabbitMQMessageBroker
import me.nathanfallet.asonar.infrastructure.messaging.handlers.FetchKeywordHandler
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for infrastructure-related dependencies: the database, the message broker, and the
 * repository implementations.
 */
val Application.infrastructureModule: Module
    get() {
        val application = this
        return module {
            // Database
            single {
                DatabaseConfig(
                    protocol = application.environment.config.property("database.protocol").getString(),
                    name = application.environment.config.property("database.name").getString(),
                    directory = application.environment.config.property("database.directory").getString(),
                    host = application.environment.config.property("database.host").getString(),
                    port = application.environment.config.property("database.port").getString().toIntOrNull() ?: 3306,
                    user = application.environment.config.property("database.user").getString(),
                    password = application.environment.config.property("database.password").getString(),
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

            // Message broker (RabbitMQ via kourier) — the Application is the connection's scope
            single<RabbitMQFactory> {
                RabbitMQFactoryImpl(
                    coroutineScope = application,
                    host = application.environment.config.property("rabbitmq.host").getString(),
                    port = application.environment.config.property("rabbitmq.port").getString().toIntOrNull() ?: 5672,
                    user = application.environment.config.property("rabbitmq.user").getString(),
                    password = application.environment.config.property("rabbitmq.password").getString(),
                )
            }
            single<MessageBroker> { RabbitMQMessageBroker(get()) }
            single { FetchKeywordHandler() }
            single<KeywordFetchQueue> { RabbitMQKeywordFetchQueue(get()) }

            // Repositories
            single<AppsRepository> { AppsDatabaseRepository(get()) }
            single<KeywordsRepository> { KeywordsDatabaseRepository(get()) }
            single<PopularitySnapshotsRepository> { PopularitySnapshotsDatabaseRepository(get()) }
            single<RankSnapshotsRepository> { RankSnapshotsDatabaseRepository(get()) }
            single<TopAppSnapshotsRepository> { TopAppSnapshotsDatabaseRepository(get()) }
        }
    }
