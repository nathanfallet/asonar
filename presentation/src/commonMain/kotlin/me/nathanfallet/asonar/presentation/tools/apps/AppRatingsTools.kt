package me.nathanfallet.asonar.presentation.tools.apps

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import me.nathanfallet.asonar.api.Serialization
import me.nathanfallet.asonar.presentation.extensions.parseStore
import me.nathanfallet.asonar.presentation.mappers.apps.toAppRatingHistoryResponse
import me.nathanfallet.asonar.presentation.routes.apps.AppRatingsRoutesDependencies
import me.nathanfallet.asonar.presentation.tools.toolError

/** Registers the app-ratings MCP tool. Shares [AppRatingsRoutesDependencies] with the HTTP route. */
fun Server.appRatingsTools(dependencies: AppRatingsRoutesDependencies) = with(dependencies) {
    addTool(
        name = "get_app_ratings",
        description = "Get an app's ratings history in a market (count + average over time) plus the ratings-per-day velocity. store is APP_STORE or PLAY_STORE; storeAppId is the store id (Apple adamId / Play package); country is ISO-2 (e.g. FR).",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("store") { put("type", "string"); put("description", "APP_STORE or PLAY_STORE.") }
                putJsonObject("storeAppId") {
                    put("type", "string"); put(
                    "description",
                    "The store app id (Apple adamId or Play package name)."
                )
                }
                putJsonObject("country") {
                    put("type", "string"); put(
                    "description",
                    "ISO-3166 alpha-2 country, e.g. FR."
                )
                }
            },
            required = listOf("store", "storeAppId", "country"),
        ),
    ) { request ->
        val storeName = request.arguments?.get("store")?.jsonPrimitive?.contentOrNull
            ?: return@addTool toolError("A \"store\" argument is required.")
        val storeAppId = request.arguments?.get("storeAppId")?.jsonPrimitive?.contentOrNull
            ?: return@addTool toolError("A \"storeAppId\" argument is required.")
        val country = request.arguments?.get("country")?.jsonPrimitive?.contentOrNull
            ?: return@addTool toolError("A \"country\" argument is required.")
        val store = parseStore(storeName)
            ?: return@addTool toolError("Unknown store: $storeName (use APP_STORE or PLAY_STORE).")
        val history = getAppRatingHistoryUseCase(store, storeAppId, country.trim().uppercase())
        CallToolResult(content = listOf(TextContent(Serialization.json.encodeToString(history.toAppRatingHistoryResponse()))))
    }
}
