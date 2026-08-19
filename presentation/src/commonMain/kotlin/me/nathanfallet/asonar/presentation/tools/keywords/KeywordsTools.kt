package me.nathanfallet.asonar.presentation.tools.keywords

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.encodeToString
import me.nathanfallet.asonar.api.Serialization
import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.presentation.mappers.keywords.toKeywordResponse
import me.nathanfallet.asonar.presentation.routes.keywords.KeywordsRoutesDependencies

/**
 * Registers the keyword MCP tools on the [Server]. Shares [KeywordsRoutesDependencies] with the HTTP
 * routes, so the tool and the endpoint answer from the exact same use case. Results are the same
 * wire DTOs as the API, serialized to JSON text.
 */
fun Server.keywordsTools(dependencies: KeywordsRoutesDependencies) = with(dependencies) {
    addTool(
        name = "list_keywords",
        description = "List the tracked keywords with their latest popularity (the 0-100 index).",
        inputSchema = ToolSchema(),
    ) {
        val overviews = listKeywordOverviewsUseCase(Pagination(limit = 100))
        val payload = Serialization.json.encodeToString(overviews.map { it.toKeywordResponse() })
        CallToolResult(content = listOf(TextContent(payload)))
    }
}
