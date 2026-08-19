package me.nathanfallet.asonar.domain.di

import me.nathanfallet.asonar.domain.usecases.apps.GetOrCreateAppUseCase
import me.nathanfallet.asonar.domain.usecases.apps.GetOrCreateAppUseCaseImpl
import me.nathanfallet.asonar.domain.usecases.keywords.GetOrCreateKeywordUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.GetOrCreateKeywordUseCaseImpl
import me.nathanfallet.asonar.domain.usecases.runs.RecordKeywordRunUseCase
import me.nathanfallet.asonar.domain.usecases.runs.RecordKeywordRunUseCaseImpl
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for domain layer dependencies: the use cases that hold asonar's business rules. Their
 * repositories are bound by the infrastructure module, which Koin resolves across modules.
 */
val domainModule: Module = module {
    single<GetOrCreateAppUseCase> { GetOrCreateAppUseCaseImpl(get()) }
    single<GetOrCreateKeywordUseCase> { GetOrCreateKeywordUseCaseImpl(get()) }
    single<RecordKeywordRunUseCase> { RecordKeywordRunUseCaseImpl(get(), get(), get()) }
}
