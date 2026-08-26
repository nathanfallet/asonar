package me.nathanfallet.asonar.presentation.config

import io.ktor.server.application.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import me.nathanfallet.asonar.presentation.routes.apps.AppCoverageRoutesDependencies
import me.nathanfallet.asonar.presentation.routes.apps.AppRatingsRoutesDependencies
import me.nathanfallet.asonar.presentation.routes.apps.AppsRoutesDependencies
import me.nathanfallet.asonar.presentation.routes.keywords.KeywordOpportunitiesRoutesDependencies
import me.nathanfallet.asonar.presentation.routes.keywords.KeywordsRoutesDependencies
import me.nathanfallet.asonar.presentation.tools.apps.appCoverageTools
import me.nathanfallet.asonar.presentation.tools.apps.appRatingsTools
import me.nathanfallet.asonar.presentation.tools.apps.appsTools
import me.nathanfallet.asonar.presentation.tools.keywords.keywordOpportunitiesTools
import me.nathanfallet.asonar.presentation.tools.keywords.keywordsTools
import org.koin.ktor.ext.get

const val MCP_SERVER_NAME = "asonar"
const val MCP_SERVER_VERSION = "1.0.0"

/**
 * Builds the MCP [Server] with every tool registered **once**. This is the single source of truth for
 * the tool catalog: both the `/mcp` mount and the web guide page read from the returned instance
 * (`server.tools`), so the guide can never drift out of sync with what's actually exposed.
 *
 * ⚠️ When you add a new `*Tools(...)` group, register it here — that's all: the guide page picks it up
 * automatically.
 */
fun mcpServer(
    appsDependencies: AppsRoutesDependencies,
    appRatingsDependencies: AppRatingsRoutesDependencies,
    appCoverageDependencies: AppCoverageRoutesDependencies,
    keywordsDependencies: KeywordsRoutesDependencies,
    keywordOpportunitiesDependencies: KeywordOpportunitiesRoutesDependencies,
): Server = Server(
    Implementation(name = MCP_SERVER_NAME, version = MCP_SERVER_VERSION),
    ServerOptions(
        capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = null)),
    ),
).apply {
    appsTools(appsDependencies)
    appRatingsTools(appRatingsDependencies)
    appCoverageTools(appCoverageDependencies)
    keywordsTools(keywordsDependencies)
    keywordOpportunitiesTools(keywordOpportunitiesDependencies)
}

/**
 * Mounts the MCP server at `POST /mcp` (Streamable HTTP). The [Server] is a Koin singleton (built once
 * by [mcpServer]), and the SDK's DSL hands that same instance to every session, so nothing is rebuilt
 * per request. DNS-rebinding protection defaults to localhost, which is exactly what a local tool
 * wants; there is no auth.
 */
fun Application.configureMcp() {
    val server = get<Server>()
    mcpStreamableHttp(path = "/mcp") { server }
}
