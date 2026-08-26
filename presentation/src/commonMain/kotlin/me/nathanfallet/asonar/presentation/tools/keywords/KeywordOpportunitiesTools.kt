package me.nathanfallet.asonar.presentation.tools.keywords

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import me.nathanfallet.asonar.api.Serialization
import me.nathanfallet.asonar.api.responses.keywords.KeywordOpportunitiesResponse
import me.nathanfallet.asonar.presentation.mappers.keywords.toKeywordOpportunityResponse
import me.nathanfallet.asonar.presentation.routes.keywords.KeywordOpportunitiesRoutesDependencies
import me.nathanfallet.asonar.presentation.tools.toolError

/** Registers the keyword-opportunities MCP tool — the "brain" surface for the agent loop. */
fun Server.keywordOpportunitiesTools(dependencies: KeywordOpportunitiesRoutesDependencies) = with(dependencies) {
    addTool(
        name = "get_keyword_opportunities",
        description = "For one of our tracked apps, score EVERY tracked keyword on its store as an ASO opportunity, best first. Each returns a verdict (YES = go for it / YES_BUT = climb as authority grows / NO = wall / RESERVE = too little volume / UNKNOWN = not enough data), a 0-100 score, and the breakdown: popularity (0-100), our current rank, our review velocity vs the top-10's median (velocityAdvantage > 1 means we out-grow the leaders → we can pass them), title/subtitle usage in the top-10, results count, plus a comment. Use it after adding candidate keywords to decide which are worth targeting. appId is asonar's internal app id (from list_apps).",
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
        val opportunities = getKeywordOpportunitiesUseCase(appId)
            ?: return@addTool toolError("App $appId not found.")
        val response = KeywordOpportunitiesResponse(opportunities.map { it.toKeywordOpportunityResponse() })
        CallToolResult(content = listOf(TextContent(Serialization.json.encodeToString(response))))
    }
}
