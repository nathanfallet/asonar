package me.nathanfallet.asonar.domain.di

import me.nathanfallet.asonar.domain.usecases.apps.DeleteAppUseCase
import me.nathanfallet.asonar.domain.usecases.apps.DeleteAppUseCaseImpl
import me.nathanfallet.asonar.domain.usecases.apps.GetAppUseCase
import me.nathanfallet.asonar.domain.usecases.apps.GetAppUseCaseImpl
import me.nathanfallet.asonar.domain.usecases.apps.GetOrCreateAppUseCase
import me.nathanfallet.asonar.domain.usecases.apps.GetOrCreateAppUseCaseImpl
import me.nathanfallet.asonar.domain.usecases.apps.ListAppsUseCase
import me.nathanfallet.asonar.domain.usecases.apps.ListAppsUseCaseImpl
import me.nathanfallet.asonar.domain.usecases.keywords.DeleteKeywordUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.DeleteKeywordUseCaseImpl
import me.nathanfallet.asonar.domain.usecases.keywords.GetKeywordDetailUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.GetKeywordDetailUseCaseImpl
import me.nathanfallet.asonar.domain.usecases.keywords.GetLatestTopAppsUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.GetLatestTopAppsUseCaseImpl
import me.nathanfallet.asonar.domain.usecases.keywords.GetOrCreateKeywordUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.GetOrCreateKeywordUseCaseImpl
import me.nathanfallet.asonar.domain.usecases.keywords.ListKeywordOverviewsUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.ListKeywordOverviewsUseCaseImpl
import me.nathanfallet.asonar.domain.usecases.keywords.ListPopularityHistoryUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.ListPopularityHistoryUseCaseImpl
import me.nathanfallet.asonar.domain.usecases.keywords.ListRankHistoryUseCase
import me.nathanfallet.asonar.domain.usecases.keywords.ListRankHistoryUseCaseImpl
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

    // Keywords
    single<ListKeywordOverviewsUseCase> { ListKeywordOverviewsUseCaseImpl(get(), get()) }
    single<GetKeywordDetailUseCase> { GetKeywordDetailUseCaseImpl(get(), get(), get(), get(), get()) }
    single<GetOrCreateKeywordUseCase> { GetOrCreateKeywordUseCaseImpl(get()) }
    single<DeleteKeywordUseCase> { DeleteKeywordUseCaseImpl(get()) }
    single<ListPopularityHistoryUseCase> { ListPopularityHistoryUseCaseImpl(get()) }
    single<GetLatestTopAppsUseCase> { GetLatestTopAppsUseCaseImpl(get()) }
    single<ListRankHistoryUseCase> { ListRankHistoryUseCaseImpl(get()) }

    // Runs — internal: written by the fetch pipeline, never exposed to the API/MCP/web.
    single<RecordKeywordRunUseCase> { RecordKeywordRunUseCaseImpl(get(), get(), get()) }
}
