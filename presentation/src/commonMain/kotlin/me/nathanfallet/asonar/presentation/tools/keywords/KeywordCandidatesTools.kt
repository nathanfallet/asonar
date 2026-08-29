package me.nathanfallet.asonar.presentation.tools.keywords

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import me.nathanfallet.asonar.api.Serialization
import me.nathanfallet.asonar.api.responses.keywords.KeywordCandidatesResponse
import me.nathanfallet.asonar.api.responses.keywords.KeywordDiscoveryResponse
import me.nathanfallet.asonar.api.responses.keywords.KeywordsResponse
import me.nathanfallet.asonar.presentation.extensions.parseCandidateStatuses
import me.nathanfallet.asonar.presentation.mappers.keywords.toKeywordCandidateResponse
import me.nathanfallet.asonar.presentation.mappers.keywords.toKeywordResponse
import me.nathanfallet.asonar.presentation.routes.keywords.KeywordCandidatesRoutesDependencies
import me.nathanfallet.asonar.presentation.tools.toolError

/** Registers the keyword-discovery MCP tools — the front of the funnel for the agent loop. */
fun Server.keywordCandidatesTools(dependencies: KeywordCandidatesRoutesDependencies) = with(dependencies) {
    addTool(
        name = "discover_keywords",
        description = "Ask the discovery sources for NEW candidate keywords for one of our apps, and store them for review. The App Store source is Apple Search Ads' own recommendations, which return each term WITH its real popularity (0-100) — so unlike terms you invent, a suggestion comes with proof it is searched. Sources expand FROM seeds: leave seeds empty to expand from the app's best-measured tracked keywords in each market, or pass your own. Leave countries empty to run every market the app already tracks. This only PROPOSES: nothing is tracked until you accept it with review_keyword_candidates. Run list_keyword_candidates afterwards to see what came out.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("appId") {
                    put("type", "number")
                    put("description", "asonar internal app id (see list_apps).")
                }
                putJsonObject("countries") {
                    put("type", "array")
                    put(
                        "description",
                        "ISO alpha-2 markets to run (e.g. [\"FR\",\"US\"]). Empty = every market the app already tracks."
                    )
                    putJsonObject("items") { put("type", "string") }
                }
                putJsonObject("seeds") {
                    put("type", "array")
                    put(
                        "description",
                        "Terms to expand from. Empty = the app's best tracked keywords per market. Seeding from a term with real volume is what returns terms with volume."
                    )
                    putJsonObject("items") { put("type", "string") }
                }
            },
            required = listOf("appId"),
        ),
    ) { request ->
        val appId = request.arguments?.get("appId")?.jsonPrimitive?.longOrNull
            ?: return@addTool toolError("An \"appId\" argument is required.")
        val countries = request.arguments?.get("countries")?.stringList()
        val seeds = request.arguments?.get("seeds")?.stringList()
        val result = discoverKeywordCandidatesUseCase(appId, countries, seeds)
            ?: return@addTool toolError("App $appId not found.")
        val response = KeywordDiscoveryResponse(
            created = result.created.map { it.toKeywordCandidateResponse() },
            updated = result.updated.map { it.toKeywordCandidateResponse() },
        )
        CallToolResult(content = listOf(TextContent(Serialization.json.encodeToString(response))))
    }

    addTool(
        name = "list_keyword_candidates",
        description = "List the keyword candidates discovery proposed for an app, best first (known popularity descending). Each carries the sources that proposed it (several sources agreeing is a relevance signal), its popularity when a source knew it, and its review state. Popularity 5 is the FLOOR of Apple's index (nobody searches it) — use minPopularity to hide it. status filters the review state (NEW / ADDED / DISMISSED, comma-separated); default NEW.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("appId") {
                    put("type", "number")
                    put("description", "asonar internal app id (see list_apps).")
                }
                putJsonObject("status") {
                    put("type", "string")
                    put("description", "Comma-separated review states to keep: NEW, ADDED, DISMISSED. Default NEW.")
                }
                putJsonObject("minPopularity") {
                    put("type", "number")
                    put(
                        "description",
                        "Drop candidates below this popularity. Candidates never measured are always kept."
                    )
                }
            },
            required = listOf("appId"),
        ),
    ) { request ->
        val appId = request.arguments?.get("appId")?.jsonPrimitive?.longOrNull
            ?: return@addTool toolError("An \"appId\" argument is required.")
        val statusRaw = request.arguments?.get("status")?.jsonPrimitive?.contentOrNull
        val statuses = parseCandidateStatuses(statusRaw)
            ?: return@addTool toolError("Unknown status: $statusRaw (use NEW, ADDED or DISMISSED).")
        val minPopularity = request.arguments?.get("minPopularity")?.jsonPrimitive?.intOrNull
        val candidates = listKeywordCandidatesUseCase(appId, statuses, minPopularity)
            ?: return@addTool toolError("App $appId not found.")
        val response = KeywordCandidatesResponse(candidates.map { it.toKeywordCandidateResponse() })
        CallToolResult(content = listOf(TextContent(Serialization.json.encodeToString(response))))
    }

    addTool(
        name = "review_keyword_candidates",
        description = "Act on candidates: accept them (start tracking each term in its market, which queues its first fetch) or dismiss them (they will never be proposed again, however many times discovery finds them). Take the ones with real popularity and on-angle for the app; dismiss the generic noise and the competitors' brand names. Returns the keywords now tracked.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("accept") {
                    put("type", "array")
                    put("description", "Candidate ids to start tracking.")
                    putJsonObject("items") { put("type", "number") }
                }
                putJsonObject("dismiss") {
                    put("type", "array")
                    put("description", "Candidate ids to bury for good.")
                    putJsonObject("items") { put("type", "number") }
                }
            },
        ),
    ) { request ->
        val accept = request.arguments?.get("accept")?.longList().orEmpty()
        val dismiss = request.arguments?.get("dismiss")?.longList().orEmpty()
        if (accept.isEmpty() && dismiss.isEmpty()) {
            return@addTool toolError("Give at least one candidate id in \"accept\" or \"dismiss\".")
        }
        reviewKeywordCandidatesUseCase.dismiss(dismiss)
        val tracked = reviewKeywordCandidatesUseCase.accept(accept)
        val response = KeywordsResponse(tracked.map { it.toKeywordResponse() })
        CallToolResult(content = listOf(TextContent(Serialization.json.encodeToString(response))))
    }
}

/** Reads a JSON array argument as strings, tolerating a single value passed instead of an array. */
private fun JsonElement.stringList(): List<String>? = when (this) {
    is JsonArray -> mapNotNull { it.jsonPrimitive.contentOrNull }.filter { it.isNotBlank() }
    is JsonPrimitive -> contentOrNull?.takeIf { it.isNotBlank() }?.let { listOf(it) }
    else -> null
}

/** Same, for ids. A value that isn't a number is dropped rather than failing the whole call. */
private fun JsonElement.longList(): List<Long>? = when (this) {
    is JsonArray -> mapNotNull { it.jsonPrimitive.longOrNull }
    is JsonPrimitive -> longOrNull?.let { listOf(it) }
    else -> null
}
