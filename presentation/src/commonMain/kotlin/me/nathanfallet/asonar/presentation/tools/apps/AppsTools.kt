package me.nathanfallet.asonar.presentation.tools.apps

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import me.nathanfallet.asonar.api.Serialization
import me.nathanfallet.asonar.domain.models.apps.AppPayload
import me.nathanfallet.asonar.presentation.extensions.parseAppRole
import me.nathanfallet.asonar.presentation.extensions.parseStore
import me.nathanfallet.asonar.presentation.mappers.apps.toAppResponse
import me.nathanfallet.asonar.presentation.routes.apps.AppsRoutesDependencies
import me.nathanfallet.asonar.presentation.tools.toolError

/** Registers the app MCP tools on the [Server]. Shares [AppsRoutesDependencies] with the HTTP routes. */
fun Server.appsTools(dependencies: AppsRoutesDependencies) = with(dependencies) {
    addTool(
        name = "list_apps",
        description = "List the apps we follow across the keyword rankings — ours (role OWNED) and the competitors we watch (role COMPETITOR).",
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
        description = "Register an app to follow (idempotent). store is APP_STORE or PLAY_STORE; storeAppId is the Apple adamId or Play package name. role is OWNED (an app we optimize, the default) or COMPETITOR (one we watch: its ranks and the keywords it indexes). Re-registering a known app with another role moves it to that role.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("store") { put("type", "string"); put("description", "APP_STORE or PLAY_STORE.") }
                putJsonObject("storeAppId") {
                    put("type", "string"); put(
                    "description",
                    "Apple adamId or Play package name."
                )
                }
                putJsonObject("name") { put("type", "string"); put("description", "A display name for the app.") }
                putJsonObject("role") {
                    put("type", "string")
                    put("description", "OWNED (default) or COMPETITOR.")
                }
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
        val roleName = request.arguments?.get("role")?.jsonPrimitive?.contentOrNull
        val role = parseAppRole(roleName)
            ?: return@addTool toolError("Unknown role: $roleName (use OWNED or COMPETITOR).")
        val app = getOrCreateAppUseCase(AppPayload(store, storeAppId, name, role))
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
