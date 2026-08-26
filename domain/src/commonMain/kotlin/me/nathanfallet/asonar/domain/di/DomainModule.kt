package me.nathanfallet.asonar.domain.di

import me.nathanfallet.asonar.domain.usecases.apps.*
import me.nathanfallet.asonar.domain.usecases.keywords.*
import me.nathanfallet.asonar.domain.usecases.runs.RecordKeywordRunUseCase
import me.nathanfallet.asonar.domain.usecases.runs.RecordKeywordRunUseCaseImpl
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for domain layer dependencies: the use cases that hold asonar's business rules. Their
 * repositories are bound by the infrastructure module, which Koin resolves across modules.
 */
val domainModule: Module = module {
    // Apps
    single<ListAppsUseCase> { ListAppsUseCaseImpl(get()) }
    single<GetAppUseCase> { GetAppUseCaseImpl(get()) }
    single<GetOrCreateAppUseCase> { GetOrCreateAppUseCaseImpl(get()) }
    single<DeleteAppUseCase> { DeleteAppUseCaseImpl(get()) }
    single<GetAppKeywordCoverageUseCase> { GetAppKeywordCoverageUseCaseImpl(get(), get(), get(), get()) }
    single<RefreshAppKeywordsUseCase> { RefreshAppKeywordsUseCaseImpl(get(), get()) }
    single<GetAppRatingHistoryUseCase> { GetAppRatingHistoryUseCaseImpl(get()) }

    // Keywords
    single<ListKeywordOverviewsUseCase> { ListKeywordOverviewsUseCaseImpl(get(), get()) }
    single<GetKeywordDetailUseCase> { GetKeywordDetailUseCaseImpl(get(), get(), get(), get(), get(), get()) }
    single<ScoreKeywordOpportunityUseCase> {
        ScoreKeywordOpportunityUseCaseImpl(
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    single<GetKeywordOpportunitiesUseCase> { GetKeywordOpportunitiesUseCaseImpl(get(), get(), get()) }
    single<GetOrCreateKeywordUseCase> { GetOrCreateKeywordUseCaseImpl(get(), get()) }
    single<DeleteKeywordUseCase> { DeleteKeywordUseCaseImpl(get()) }
    single<ListPopularityHistoryUseCase> { ListPopularityHistoryUseCaseImpl(get()) }
    single<GetLatestTopAppsUseCase> { GetLatestTopAppsUseCaseImpl(get()) }
    single<ListRankHistoryUseCase> { ListRankHistoryUseCaseImpl(get()) }
    single<RefreshKeywordUseCase> { RefreshKeywordUseCaseImpl(get(), get()) }

    // Runs — internal: written by the fetch pipeline, never exposed to the API/MCP/web.
    single<RecordKeywordRunUseCase> { RecordKeywordRunUseCaseImpl(get(), get(), get(), get()) }
    single<FetchKeywordUseCase> {
        FetchKeywordUseCaseImpl(get(), get(), getAll(), getAll(), getAll(), get(), get(), get(), get(), get())
    }
}
