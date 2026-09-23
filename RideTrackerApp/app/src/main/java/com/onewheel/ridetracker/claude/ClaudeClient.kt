package com.onewheel.ridetracker.claude

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

sealed class ClaudeResult {
    data class Success(val text: String) : ClaudeResult()
    data class Failure(val message: String) : ClaudeResult()
}

/** Thin client for the Anthropic Messages API — no SDK dependency, just HttpURLConnection + org.json. */
object ClaudeClient {

    private const val ENDPOINT = "https://api.anthropic.com/v1/messages"
    private const val API_VERSION = "2023-06-01"
    private const val MODEL = "claude-sonnet-4-5" // update here if you want a different/newer model

    /** Convenience for a single-turn question with no prior history. */
    suspend fun sendMessage(apiKey: String, prompt: String): ClaudeResult =
        sendConversation(apiKey, listOf("user" to prompt))

    /** [messages] is the full conversation so far as (role, text) pairs — "user" / "assistant". */
    suspend fun sendConversation(apiKey: String, messages: List<Pair<String, String>>): ClaudeResult = withContext(Dispatchers.IO) {
        try {
            val url = URL(ENDPOINT)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
            conn.setRequestProperty("content-type", "application/json")
            conn.setRequestProperty("x-api-key", apiKey)
            conn.setRequestProperty("anthropic-version", API_VERSION)

            val body = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", 1536)
                put("messages", JSONArray().apply {
                    messages.forEach { (role, text) ->
                        put(JSONObject().apply {
                            put("role", role)
                            put("content", text)
                        })
                    }
                })
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""

            if (status !in 200..299) {
                val message = try {
                    JSONObject(responseText).optJSONObject("error")?.optString("message")
                } catch (e: Exception) { null }
                return@withContext ClaudeResult.Failure(message ?: "Request failed (HTTP $status).")
            }

            val json = JSONObject(responseText)
            val contentArray = json.optJSONArray("content") ?: JSONArray()
            val text = buildString {
                for (i in 0 until contentArray.length()) {
                    val block = contentArray.optJSONObject(i) ?: continue
                    if (block.optString("type") == "text") {
                        append(block.optString("text"))
                    }
                }
            }.trim()

            if (text.isBlank()) {
                ClaudeResult.Failure("Claude returned an empty response.")
            } else {
                ClaudeResult.Success(text)
            }
        } catch (e: Exception) {
            ClaudeResult.Failure(e.message ?: "Network error — check your connection and try again.")
        }
    }
}
