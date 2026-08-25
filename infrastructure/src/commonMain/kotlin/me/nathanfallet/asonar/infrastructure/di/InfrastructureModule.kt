package me.nathanfallet.asonar.infrastructure.di

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import me.nathanfallet.asonar.api.Serialization
import me.nathanfallet.asonar.domain.repositories.*
import me.nathanfallet.asonar.domain.services.*
import me.nathanfallet.asonar.infrastructure.database.*
import me.nathanfallet.asonar.infrastructure.database.repositories.*
import me.nathanfallet.asonar.infrastructure.health.DatabaseHealthService
import me.nathanfallet.asonar.infrastructure.messaging.*
import me.nathanfallet.asonar.infrastructure.messaging.handlers.FetchKeywordHandler
import me.nathanfallet.asonar.infrastructure.scraping.AppStoreSubtitleSource
import me.nathanfallet.asonar.infrastructure.scraping.AsaKeywordPopularitySource
import me.nathanfallet.asonar.infrastructure.scraping.BrowserHolder
import me.nathanfallet.asonar.infrastructure.scraping.ItunesAppSearchSource
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
            single { FetchKeywordHandler(get()) }
            single<KeywordFetchQueue> { RabbitMQKeywordFetchQueue(get()) }

            // Outbound HTTP + the per-store data sources the fetch orchestrator selects from
            single {
                HttpClient(CIO) {
                    install(ContentNegotiation) { json(Serialization.json) }
                }
            }
            single<AppSearchSource> { ItunesAppSearchSource(get()) }
            single<AppSubtitleSource> { AppStoreSubtitleSource(get()) }
            single {
                BrowserHolder(
                    scope = application,
                    profileDir = application.environment.config.property("asa.profileDir").getString(),
                    baseUrl = application.environment.config.property("asa.baseUrl").getString(),
                )
            }
            single<KeywordPopularitySource> {
                AsaKeywordPopularitySource(
                    browserHolder = get(),
                    adamId = application.environment.config.property("asa.adamId").getString(),
                    graphqlEndpoint = application.environment.config.property("asa.graphqlEndpoint").getString(),
                )
            }

            // Repositories
            single<AppsRepository> { AppsDatabaseRepository(get()) }
            single<KeywordsRepository> { KeywordsDatabaseRepository(get()) }
            single<PopularitySnapshotsRepository> { PopularitySnapshotsDatabaseRepository(get()) }
            single<RankSnapshotsRepository> { RankSnapshotsDatabaseRepository(get()) }
            single<TopAppSnapshotsRepository> { TopAppSnapshotsDatabaseRepository(get()) }
            single<AppRatingSnapshotsRepository> { AppRatingSnapshotsDatabaseRepository(get()) }
        }
    }
