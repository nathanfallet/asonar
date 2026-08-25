package me.nathanfallet.asonar.presentation.config

import io.ktor.server.application.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import me.nathanfallet.asonar.presentation.routes.apps.AppRatingsRoutesDependencies
import me.nathanfallet.asonar.presentation.routes.apps.AppsRoutesDependencies
import me.nathanfallet.asonar.presentation.routes.keywords.KeywordsRoutesDependencies
import me.nathanfallet.asonar.presentation.tools.apps.appRatingsTools
import me.nathanfallet.asonar.presentation.tools.apps.appsTools
import me.nathanfallet.asonar.presentation.tools.keywords.keywordsTools
import org.koin.ktor.ext.get

const val MCP_SERVER_NAME = "asonar"
const val MCP_SERVER_VERSION = "1.0.0"

/**
 * Mounts the MCP server at `POST /mcp` (Streamable HTTP). The [Server] is built **once** here — the
 * tools are registered a single time — and the SDK's DSL hands that same instance to every session,
 * so nothing is rebuilt per request. DNS-rebinding protection defaults to localhost, which is
 * exactly what a local tool wants; there is no auth.
 */
fun Application.configureMcp() {
    val server = Server(
        Implementation(name = MCP_SERVER_NAME, version = MCP_SERVER_VERSION),
        ServerOptions(
            capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = null)),
        ),
    ).apply {
        appsTools(get<AppsRoutesDependencies>())
        appRatingsTools(get<AppRatingsRoutesDependencies>())
        keywordsTools(get<KeywordsRoutesDependencies>())
    }
    mcpStreamableHttp(path = "/mcp") { server }
}
