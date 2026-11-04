package net.portswigger.mcp.tools

import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ContentBlock
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import net.portswigger.mcp.schema.asInputSchema
import kotlin.experimental.ExperimentalTypeInference

@PublishedApi
internal fun annotationsFor(name: String): ToolAnnotations = when (name) {
    "url_encode", "url_decode", "base64_encode", "base64_decode", "generate_random_string",
    "output_project_options", "output_user_options", "get_scanner_issues",
    "get_proxy_http_history", "get_proxy_http_history_regex", "get_organizer_items",
    "get_organizer_items_regex", "get_proxy_websocket_history", "get_proxy_websocket_history_regex",
    "get_collaborator_interactions", "get_active_editor_contents" ->
        ToolAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false)

    "create_repeater_tab", "create_repeater_tab_http2", "send_to_intruder",
    "generate_collaborator_payload" ->
        ToolAnnotations(readOnlyHint = false, destructiveHint = false, openWorldHint = false)

    "set_project_options", "set_user_options", "set_task_execution_engine_state",
    "set_proxy_intercept_state", "set_active_editor_contents" ->
        ToolAnnotations(readOnlyHint = false, destructiveHint = true, openWorldHint = false)

    "send_http1_request", "send_http2_request" ->
        ToolAnnotations(readOnlyHint = false, destructiveHint = true, openWorldHint = true)

    else -> error("Missing tool annotations for $name")
}

@OptIn(InternalSerializationApi::class)
inline fun <reified I : Any> Server.mcpTool(
    description: String,
    crossinline execute: suspend I.() -> List<ContentBlock>
) {
    val toolName = I::class.simpleName?.toLowerSnakeCase() ?: error("Couldn't find name for ${I::class}")
    val serializer = I::class.serializer()
    val inputSchema = I::class.asInputSchema()

    val handler: suspend (ClientConnection, CallToolRequest) -> CallToolResult = { _, request ->
        try {
            CallToolResult(
                content = execute(
                    Json.decodeFromJsonElement(
                        serializer,
                        request.params.arguments ?: JsonObject(emptyMap())
                    )
                ),
                isError = false
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent("Error: ${e.message}")),
                isError = true
            )
        }
    }

    addTool(name = toolName, description = description, inputSchema = inputSchema,
        toolAnnotations = annotationsFor(toolName), handler = handler)
}

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("mcpToolString")
inline fun <reified I : Any> Server.mcpTool(
    description: String,
    crossinline execute: suspend I.() -> String
) {
    mcpTool<I>(description, execute = {
        listOf(TextContent(execute(this)))
    })
}

inline fun <reified I : Any> Server.mcpUnitTool(
    description: String,
    crossinline execute: suspend I.() -> Unit
) {
    mcpTool<I>(description, execute = {
        execute(this)

        listOf(TextContent("Executed tool"))
    })
}

inline fun <reified I : Paginated, J : Any> Server.mcpPaginatedTool(
    description: String,
    noinline mapper: (J) -> CharSequence = { it.toString() },
    crossinline execute: suspend I.() -> List<J>
) {
    mcpTool<I>(description, execute = {

        val items = execute(this)

        when {
            offset >= items.size -> {
                "Reached end of items"
            }

            else -> {
                val upperLimit = (offset + count).coerceAtMost(items.size)

                items.subList(offset, upperLimit)
                    .joinToString(separator = "\n\n", transform = mapper)
            }
        }
    })
}

inline fun <reified I : Paginated> Server.mcpPaginatedTool(
    description: String,
    crossinline execute: suspend I.() -> Sequence<String>
) {
    mcpTool<I>(description, execute = {
        val seq = execute(this)
        val paginated = seq.drop(offset).take(count).toList()

        if (paginated.isEmpty()) {
            listOf(TextContent("Reached end of items"))
        } else {
            listOf(TextContent(paginated.joinToString(separator = "\n\n")))
        }
    })
}

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("mcpNamedToolString")
inline fun Server.mcpTool(
    name: String,
    description: String,
    crossinline execute: suspend () -> List<ContentBlock>
) {
    val handler: suspend (ClientConnection, CallToolRequest) -> CallToolResult = { _, _ ->
        CallToolResult(content = execute(), isError = false)
    }
    addTool(name = name, description = description, inputSchema = ToolSchema(),
        toolAnnotations = annotationsFor(name), handler = handler)
}

inline fun Server.mcpTool(
    name: String,
    description: String,
    crossinline execute: suspend () -> String
) {
    val handler: suspend (ClientConnection, CallToolRequest) -> CallToolResult = { _, _ ->
        CallToolResult(content = listOf(TextContent(execute())), isError = false)
    }
    addTool(name = name, description = description, inputSchema = ToolSchema(),
        toolAnnotations = annotationsFor(name), handler = handler)
}

fun String.toLowerSnakeCase(): String {
    return this
        .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
        .replace(Regex("([A-Z])([A-Z][a-z])"), "$1_$2")
        .replace(Regex("[\\s-]+"), "_")
        .lowercase()
}

interface Paginated {
    val count: Int
    val offset: Int
}
