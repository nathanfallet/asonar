package me.nathanfallet.asonar.presentation.tools.apps

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import me.nathanfallet.asonar.api.Serialization
import me.nathanfallet.asonar.domain.models.apps.AppPayload
import me.nathanfallet.asonar.presentation.extensions.parseStore
import me.nathanfallet.asonar.presentation.tools.toolError
import me.nathanfallet.asonar.presentation.mappers.apps.toAppResponse
import me.nathanfallet.asonar.presentation.routes.apps.AppsRoutesDependencies

/** Registers the app MCP tools on the [Server]. Shares [AppsRoutesDependencies] with the HTTP routes. */
fun Server.appsTools(dependencies: AppsRoutesDependencies) = with(dependencies) {
    addTool(
        name = "list_apps",
        description = "List the apps we optimize and follow across the keyword rankings.",
        inputSchema = ToolSchema(),
    ) {
        CallToolResult(content = listOf(TextContent(Serialization.json.encodeToString(listAppsUseCase().map { it.toAppResponse() }))))
    }

    addTool(
        name = "get_app",
        description = "Get one app by its id.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("appId") { put("type", "integer"); put("description", "The id of the app.") }
            },
            required = listOf("appId"),
        ),
    ) { request ->
        val id = request.arguments?.get("appId")?.jsonPrimitive?.longOrNull
            ?: return@addTool toolError("A valid integer \"appId\" is required.")
        val app = getAppUseCase(id) ?: return@addTool toolError("No app found with id $id.")
        CallToolResult(content = listOf(TextContent(Serialization.json.encodeToString(app.toAppResponse()))))
    }

    addTool(
        name = "register_app",
        description = "Register an app to follow (idempotent). store is APP_STORE or PLAY_STORE; storeAppId is the Apple adamId or Play package name.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("store") { put("type", "string"); put("description", "APP_STORE or PLAY_STORE.") }
                putJsonObject("storeAppId") { put("type", "string"); put("description", "Apple adamId or Play package name.") }
                putJsonObject("name") { put("type", "string"); put("description", "A display name for the app.") }
            },
            required = listOf("store", "storeAppId", "name"),
        ),
    ) { request ->
        val storeName = request.arguments?.get("store")?.jsonPrimitive?.contentOrNull
            ?: return@addTool toolError("A \"store\" argument is required.")
        val storeAppId = request.arguments?.get("storeAppId")?.jsonPrimitive?.contentOrNull
            ?: return@addTool toolError("A \"storeAppId\" argument is required.")
        val name = request.arguments?.get("name")?.jsonPrimitive?.contentOrNull
            ?: return@addTool toolError("A \"name\" argument is required.")
        val store = parseStore(storeName)
            ?: return@addTool toolError("Unknown store: $storeName (use APP_STORE or PLAY_STORE).")
        val app = getOrCreateAppUseCase(AppPayload(store, storeAppId, name))
        CallToolResult(content = listOf(TextContent(Serialization.json.encodeToString(app.toAppResponse()))))
    }

    addTool(
        name = "delete_app",
        description = "Stop following an app.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("appId") { put("type", "integer"); put("description", "The id of the app to remove.") }
            },
            required = listOf("appId"),
        ),
    ) { request ->
        val id = request.arguments?.get("appId")?.jsonPrimitive?.longOrNull
            ?: return@addTool toolError("A valid integer \"appId\" is required.")
        if (deleteAppUseCase(id)) CallToolResult(content = listOf(TextContent("App $id removed.")))
        else toolError("No app found with id $id.")
    }
}
