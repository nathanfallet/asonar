package me.nathanfallet.asonar.presentation.routes.web

import io.ktor.server.freemarker.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.usecases.keywords.ListKeywordOverviewsUseCase
import me.nathanfallet.asonar.presentation.views.DashboardView
import me.nathanfallet.asonar.presentation.views.KeywordRowView
import me.nathanfallet.asonar.presentation.views.LayoutView

/** The server-rendered web UI. For now: the dashboard of tracked keywords. */
fun Route.webRoutes(listKeywordOverviewsUseCase: ListKeywordOverviewsUseCase) {
    get("/") {
        val rows = listKeywordOverviewsUseCase(Pagination(limit = 100)).map { overview ->
            KeywordRowView(
                term = overview.keyword.term,
                store = overview.keyword.store.name,
                country = overview.keyword.country,
                popularityLabel = overview.latestPopularity?.popularity?.toString() ?: "—",
                popularityValue = overview.latestPopularity?.popularity ?: 0,
                hasPopularity = overview.latestPopularity != null,
                capturedAt = overview.latestPopularity?.capturedAt?.toString()
                    ?.take(16)?.replace("T", " ") ?: "—",
            )
        }
        call.respond(
            FreeMarkerContent(
                "dashboard.ftl",
                mapOf("view" to DashboardView(LayoutView("Dashboard"), rows)),
            )
        )
    }
}
