package me.nathanfallet.asonar.domain.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for domain layer dependencies: the use cases that hold asonar's business rules.
 *
 * Empty for now — filled in as we add the keyword-tracking use cases.
 */
val domainModule: Module = module {
    // Use cases go here, e.g.
    // single<TrackKeywordUseCase> { TrackKeywordUseCaseImpl(get()) }
}
