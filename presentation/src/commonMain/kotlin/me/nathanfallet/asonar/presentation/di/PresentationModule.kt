package me.nathanfallet.asonar.presentation.di

import me.nathanfallet.asonar.presentation.routes.keywords.KeywordsRoutesDependencies
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for presentation layer dependencies — the per-resource dependency bundles shared by
 * the HTTP routes and the MCP tools.
 */
val presentationModule: Module = module {
    single { KeywordsRoutesDependencies(get()) }
}
