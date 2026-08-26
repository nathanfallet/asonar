package me.nathanfallet.asonar.presentation.tools.apps

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import me.nathanfallet.asonar.api.Serialization
import me.nathanfallet.asonar.presentation.mappers.apps.toAppKeywordCoverageResponse
import me.nathanfallet.asonar.presentation.routes.apps.AppCoverageRoutesDependencies
import me.nathanfallet.asonar.presentation.tools.toolError

/** Registers the app-coverage MCP tool. Shares [AppCoverageRoutesDependencies] with the HTTP route. */
fun Server.appCoverageTools(dependencies: AppCoverageRoutesDependencies) = with(dependencies) {
    addTool(
        name = "get_app_coverage",
        description = "For one of our tracked apps, list every tracked keyword on its store with our CURRENT rank (rank is null = we don't rank in the scanned results), the keyword's popularity (0-100), and the full rank history. Use it to see, in one call, which keywords the app ranks on and which are still to chase. appId is asonar's internal app id (from list_apps).",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("appId") {
                    put("type", "number")
                    put("description", "asonar internal app id (see list_apps).")
                }
            },
            required = listOf("appId"),
        ),
    ) { request ->
        val appId = request.arguments?.get("appId")?.jsonPrimitive?.longOrNull
            ?: return@addTool toolError("An \"appId\" argument is required.")
        val coverage = getAppKeywordCoverageUseCase(appId)
            ?: return@addTool toolError("App $appId not found.")
        // Viewing the coverage kicks off a background refresh of the relevant keywords (gated to stay cheap).
        refreshAppKeywordsUseCase(appId)
        CallToolResult(content = listOf(TextContent(Serialization.json.encodeToString(coverage.toAppKeywordCoverageResponse()))))
    }
}
