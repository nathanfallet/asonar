package me.nathanfallet.asonar.presentation.tools.keywords

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
import me.nathanfallet.asonar.domain.models.application.Pagination
import me.nathanfallet.asonar.domain.models.keywords.KeywordPayload
import me.nathanfallet.asonar.presentation.extensions.parseStore
import me.nathanfallet.asonar.presentation.tools.toolError
import me.nathanfallet.asonar.presentation.mappers.keywords.toKeywordDetailResponse
import me.nathanfallet.asonar.presentation.mappers.keywords.toKeywordResponse
import me.nathanfallet.asonar.presentation.mappers.snapshots.toPopularitySnapshotResponse
import me.nathanfallet.asonar.presentation.mappers.snapshots.toRankSnapshotResponse
import me.nathanfallet.asonar.presentation.mappers.snapshots.toTopAppSnapshotResponse
import me.nathanfallet.asonar.presentation.routes.keywords.KeywordsRoutesDependencies

/**
 * Registers the keyword MCP tools on the [Server]. Shares [KeywordsRoutesDependencies] with the HTTP
 * routes, so each tool answers from the exact same use case, and returns the same wire DTOs as JSON
 * text. There is deliberately no tool to write fetched data — that stays with the fetch pipeline.
 */
fun Server.keywordsTools(dependencies: KeywordsRoutesDependencies) = with(dependencies) {
    addTool(
        name = "list_keywords",
        description = "List the tracked keywords with their latest popularity (the 0-100 index).",
        inputSchema = ToolSchema(),
    ) {
        val overviews = listKeywordOverviewsUseCase(Pagination(limit = 0))
        CallToolResult(
            content = listOf(TextContent(Serialization.json.encodeToString(overviews.map { it.toKeywordResponse() }))),
        )
    }

    addTool(
        name = "get_keyword",
        description = "Get one keyword's full detail: latest popularity, top-of-results and our apps' ranks.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("keywordId") { put("type", "integer"); put("description", "The id of the keyword.") }
            },
            required = listOf("keywordId"),
        ),
    ) { request ->
        val id = request.arguments?.get("keywordId")?.jsonPrimitive?.longOrNull
            ?: return@addTool toolError("A valid integer \"keywordId\" is required.")
        val detail = getKeywordDetailUseCase(id)
            ?: return@addTool toolError("No keyword found with id $id.")
        CallToolResult(content = listOf(TextContent(Serialization.json.encodeToString(detail.toKeywordDetailResponse()))))
    }

    addTool(
        name = "track_keyword",
        description = "Start tracking a keyword (idempotent). store is APP_STORE or PLAY_STORE; country is ISO-2 (e.g. FR). Tracking a NEW keyword automatically queues an initial fetch of its data — no need to call refresh_keyword afterwards.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("term") { put("type", "string"); put("description", "The search term.") }
                putJsonObject("store") { put("type", "string"); put("description", "APP_STORE or PLAY_STORE.") }
                putJsonObject("country") { put("type", "string"); put("description", "ISO-3166 alpha-2 country, e.g. FR.") }
            },
            required = listOf("term", "store", "country"),
        ),
    ) { request ->
        val term = request.arguments?.get("term")?.jsonPrimitive?.contentOrNull
            ?: return@addTool toolError("A \"term\" argument is required.")
        val storeName = request.arguments?.get("store")?.jsonPrimitive?.contentOrNull
            ?: return@addTool toolError("A \"store\" argument is required.")
        val country = request.arguments?.get("country")?.jsonPrimitive?.contentOrNull
            ?: return@addTool toolError("A \"country\" argument is required.")
        val store = parseStore(storeName)
            ?: return@addTool toolError("Unknown store: $storeName (use APP_STORE or PLAY_STORE).")
        val keyword = getOrCreateKeywordUseCase(KeywordPayload(term, store, country))
        CallToolResult(content = listOf(TextContent(Serialization.json.encodeToString(keyword.toKeywordResponse()))))
    }

    addTool(
        name = "untrack_keyword",
        description = "Stop tracking a keyword.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("keywordId") { put("type", "integer"); put("description", "The id of the keyword to remove.") }
            },
            required = listOf("keywordId"),
        ),
    ) { request ->
        val id = request.arguments?.get("keywordId")?.jsonPrimitive?.longOrNull
            ?: return@addTool toolError("A valid integer \"keywordId\" is required.")
        if (deleteKeywordUseCase(id)) CallToolResult(content = listOf(TextContent("Keyword $id removed.")))
        else toolError("No keyword found with id $id.")
    }

    addTool(
        name = "get_keyword_popularity_history",
        description = "Get a keyword's popularity history (newest first).",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("keywordId") { put("type", "integer"); put("description", "The id of the keyword.") }
            },
            required = listOf("keywordId"),
        ),
    ) { request ->
        val id = request.arguments?.get("keywordId")?.jsonPrimitive?.longOrNull
            ?: return@addTool toolError("A valid integer \"keywordId\" is required.")
        val history = listPopularityHistoryUseCase(id, Pagination(limit = 100))
        CallToolResult(content = listOf(TextContent(Serialization.json.encodeToString(history.map { it.toPopularitySnapshotResponse() }))))
    }

    addTool(
        name = "get_keyword_top_apps",
        description = "Get a keyword's most recent top-of-results (the competing apps).",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("keywordId") { put("type", "integer"); put("description", "The id of the keyword.") }
            },
            required = listOf("keywordId"),
        ),
    ) { request ->
        val id = request.arguments?.get("keywordId")?.jsonPrimitive?.longOrNull
            ?: return@addTool toolError("A valid integer \"keywordId\" is required.")
        val top = getLatestTopAppsUseCase(id)
        CallToolResult(content = listOf(TextContent(Serialization.json.encodeToString(top.map { it.toTopAppSnapshotResponse() }))))
    }

    addTool(
        name = "get_keyword_ranks",
        description = "Get the rank history of one of our apps on a keyword (newest first).",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("keywordId") { put("type", "integer"); put("description", "The id of the keyword.") }
                putJsonObject("appId") { put("type", "integer"); put("description", "The id of the app.") }
            },
            required = listOf("keywordId", "appId"),
        ),
    ) { request ->
        val keywordId = request.arguments?.get("keywordId")?.jsonPrimitive?.longOrNull
            ?: return@addTool toolError("A valid integer \"keywordId\" is required.")
        val appId = request.arguments?.get("appId")?.jsonPrimitive?.longOrNull
            ?: return@addTool toolError("A valid integer \"appId\" is required.")
        val history = listRankHistoryUseCase(keywordId, appId, Pagination(limit = 100))
        CallToolResult(content = listOf(TextContent(Serialization.json.encodeToString(history.map { it.toRankSnapshotResponse() }))))
    }

    addTool(
        name = "refresh_keyword",
        description = "Queue a fetch of a keyword's popularity/rank data. Returns immediately; the fetch runs asynchronously.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("keywordId") { put("type", "integer"); put("description", "The id of the keyword to refresh.") }
            },
            required = listOf("keywordId"),
        ),
    ) { request ->
        val id = request.arguments?.get("keywordId")?.jsonPrimitive?.longOrNull
            ?: return@addTool toolError("A valid integer \"keywordId\" is required.")
        if (refreshKeywordUseCase(id)) CallToolResult(content = listOf(TextContent("Fetch queued for keyword $id.")))
        else toolError("No keyword found with id $id.")
    }
}
