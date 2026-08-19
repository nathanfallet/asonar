package me.nathanfallet.asonar.presentation.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for presentation layer dependencies (mappers, view models).
 *
 * Empty for now — routes read their use cases straight from Koin.
 */
val presentationModule: Module = module {
}
