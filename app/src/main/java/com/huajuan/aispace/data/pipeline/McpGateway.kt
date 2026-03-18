package com.huajuan.aispace.data.pipeline

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class McpToolDefinition(
    val name: String,
    val description: String = ""
)

data class McpCallResult(
    val success: Boolean,
    val outputText: String
)

internal class McpGateway {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun listTools(serverUrl: String): List<McpToolDefinition> {
        return when {
            serverUrl.startsWith("http://") || serverUrl.startsWith("https://") -> listToolsHttp(serverUrl)
            else -> listToolsStdio(serverUrl)
        }
    }

    fun callTool(serverUrl: String, toolName: String, args: Map<String, Any?>): McpCallResult {
        return when {
            serverUrl.startsWith("http://") || serverUrl.startsWith("https://") -> callToolHttp(serverUrl, toolName, args)
            else -> callToolStdio(serverUrl, toolName, args)
        }
    }

    private fun listToolsHttp(serverUrl: String): List<McpToolDefinition> {
        val payload = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            addProperty("id", 1)
            addProperty("method", "tools/list")
        }
        val req = Request.Builder()
            .url(serverUrl)
            .addHeader("Accept", "application/json, text/event-stream")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        return client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val text = resp.body?.string().orEmpty()
            parseToolsFromJsonRpcText(text)
        }
    }

    private fun callToolHttp(serverUrl: String, toolName: String, args: Map<String, Any?>): McpCallResult {
        val argsObj = JsonObject().apply {
            args.forEach { (key, value) ->
                when (value) {
                    null -> add(key, JsonNull.INSTANCE)
                    is Number -> addProperty(key, value)
                    is Boolean -> addProperty(key, value)
                    else -> addProperty(key, value.toString())
                }
            }
        }
        val payload = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            addProperty("id", 1)
            addProperty("method", "tools/call")
            add("params", JsonObject().apply {
                addProperty("name", toolName)
                add("arguments", argsObj)
            })
        }
        val req = Request.Builder()
            .url(serverUrl)
            .addHeader("Accept", "application/json, text/event-stream")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        return client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                return McpCallResult(false, "HTTP ${resp.code}: ${resp.message}")
            }
            val text = resp.body?.string().orEmpty()
            parseToolCallResultText(text)
        }
    }

    private fun listToolsStdio(commandLine: String): List<McpToolDefinition> {
        return try {
            val (program, args) = splitCommand(commandLine)
            val process = ProcessBuilder(listOf(program) + args).start()
            process.outputStream.bufferedWriter().use { writer ->
                writer.write(
                    """{"jsonrpc":"2.0","id":1,"method":"tools/list"}"""
                )
                writer.newLine()
                writer.flush()
            }
            val text = process.inputStream.bufferedReader().readText()
            process.waitFor(8, TimeUnit.SECONDS)
            parseToolsFromJsonRpcText(text)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun callToolStdio(serverUrl: String, toolName: String, args: Map<String, Any?>): McpCallResult {
        return try {
            val argsObj = JsonObject().apply {
                args.forEach { (key, value) ->
                when (value) {
                        null -> add(key, JsonNull.INSTANCE)
                        is Number -> addProperty(key, value)
                        is Boolean -> addProperty(key, value)
                        else -> addProperty(key, value.toString())
                    }
                }
            }
            val payload = JsonObject().apply {
                addProperty("jsonrpc", "2.0")
                addProperty("id", 1)
                addProperty("method", "tools/call")
                add("params", JsonObject().apply {
                    addProperty("name", toolName)
                    add("arguments", argsObj)
                })
            }
            val (program, procArgs) = splitCommand(serverUrl)
            val process = ProcessBuilder(listOf(program) + procArgs).start()
            process.outputStream.bufferedWriter().use { writer ->
                writer.write(payload.toString())
                writer.newLine()
                writer.flush()
            }
            val out = BufferedReader(InputStreamReader(process.inputStream)).readText()
            process.waitFor(10, TimeUnit.SECONDS)
            parseToolCallResultText(out)
        } catch (e: Exception) {
            McpCallResult(false, e.message ?: "stdio call failed")
        }
    }

    private fun parseToolsFromJsonRpcText(text: String): List<McpToolDefinition> {
        val payload = parseLastJsonObject(text) ?: return emptyList()
        val tools = payload.getAsJsonObject("result")?.getAsJsonArray("tools") ?: JsonArray()
        return tools.mapNotNull { el ->
            if (!el.isJsonObject) return@mapNotNull null
            val obj = el.asJsonObject
            val name = obj.get("name")?.asString.orEmpty()
            if (name.isBlank()) return@mapNotNull null
            McpToolDefinition(name = name, description = obj.get("description")?.asString.orEmpty())
        }
    }

    private fun parseToolCallResultText(text: String): McpCallResult {
        val payload = parseLastJsonObject(text) ?: return McpCallResult(false, text.take(1000))
        val err = payload.getAsJsonObject("error")
        if (err != null) {
            return McpCallResult(false, err.get("message")?.asString ?: "mcp error")
        }
        val result = payload.getAsJsonObject("result")
        val content = result?.get("content")
        return McpCallResult(
            success = true,
            outputText = when {
                content == null || content.isJsonNull -> result?.toString().orEmpty()
                content.isJsonPrimitive -> content.asString
                content.isJsonArray -> content.toString()
                content.isJsonObject -> content.toString()
                else -> result?.toString().orEmpty()
            }.take(2000)
        )
    }

    private fun splitCommand(raw: String): Pair<String, List<String>> {
        val normalized = raw.removePrefix("stdio://").trim()
        val parts = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        return (parts.firstOrNull() ?: raw) to parts.drop(1)
    }

    private fun parseLastJsonObject(text: String): JsonObject? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return null
        if (trimmed.startsWith("{")) {
            return runCatching { JsonParser.parseString(trimmed).asJsonObject }.getOrNull()
        }
        val lines = trimmed.lines().asReversed()
        for (line in lines) {
            val payload = line.trim().removePrefix("data:").trim()
            if (!payload.startsWith("{")) continue
            val parsed = runCatching { JsonParser.parseString(payload).asJsonObject }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }
}
