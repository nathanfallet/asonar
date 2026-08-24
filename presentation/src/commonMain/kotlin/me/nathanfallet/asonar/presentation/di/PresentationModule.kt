package me.nathanfallet.asonar.presentation.di

import me.nathanfallet.asonar.presentation.routes.apps.AppsRoutesDependencies
import me.nathanfallet.asonar.presentation.routes.keywords.KeywordsRoutesDependencies
import me.nathanfallet.asonar.presentation.routes.web.WebRoutesDependencies
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for presentation layer dependencies — the per-resource dependency bundles shared by
 * the HTTP routes and the MCP tools.
 */
val presentationModule: Module = module {
    single { AppsRoutesDependencies(get(), get(), get(), get()) }
    single { KeywordsRoutesDependencies(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { WebRoutesDependencies(get(), get(), get(), get()) }
}
