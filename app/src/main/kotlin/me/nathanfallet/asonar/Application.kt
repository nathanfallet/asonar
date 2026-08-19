package me.nathanfallet.asonar

import io.ktor.server.application.*
import io.ktor.server.netty.*
import me.nathanfallet.asonar.domain.di.domainModule
import me.nathanfallet.asonar.infrastructure.di.infrastructureModule
import me.nathanfallet.asonar.presentation.config.configureErrorHandling
import me.nathanfallet.asonar.presentation.config.configureMcp
import me.nathanfallet.asonar.presentation.config.configureMonitoring
import me.nathanfallet.asonar.presentation.config.configureRouting
import me.nathanfallet.asonar.presentation.config.configureSerialization
import me.nathanfallet.asonar.presentation.config.configureTemplating
import me.nathanfallet.asonar.presentation.di.presentationModule
import org.koin.ktor.plugin.Koin

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    install(Koin) {
        modules(
            domainModule,
            presentationModule,
            infrastructureModule,
        )
    }
    configureSerialization()
    configureErrorHandling()
    configureMonitoring()
    configureTemplating()
    configureRouting()
    configureMcp()
}
