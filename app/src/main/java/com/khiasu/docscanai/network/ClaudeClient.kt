package com.khiasu.docscanai.network

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Anthropic Claude messages API vision client. */
class ClaudeClient(
    private val model: String = "claude-sonnet-4-6"
) : AiClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun extract(apiKey: String, bitmap: Bitmap): Result<ExtractionResult> =
        withContext(Dispatchers.IO) {
            try {
                val base64Image = bitmapToBase64Jpeg(bitmap)

                val body = JSONObject().apply {
                    put("model", model)
                    put("max_tokens", 2048)
                    put("messages", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("content", JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "image")
                                put("source", JSONObject().apply {
                                    put("type", "base64")
                                    put("media_type", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                            put(JSONObject().put("type", "text").put("text", EXTRACTION_PROMPT))
                        })
                    }))
                }

                val request = Request.Builder()
                    .url("https://api.anthropic.com/v1/messages")
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                http.newCall(request).execute().use { resp ->
                    val respBody = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(Exception("Claude error ${resp.code}: $respBody"))
                    }
                    val json = JSONObject(respBody)
                    val text = json.getJSONArray("content").getJSONObject(0).getString("text")
                    val usage = json.optJSONObject("usage")
                    val promptTokens = usage?.optInt("input_tokens") ?: 0
                    val completionTokens = usage?.optInt("output_tokens") ?: 0
                    val base = parseExtractionJson(text)
                    Result.success(base.copy(promptTokens = promptTokens, completionTokens = completionTokens))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun solve(apiKey: String, rawText: String): Result<ExtractionResult> =
        withContext(Dispatchers.IO) {
            try {
                val prompt = SOLVE_PROMPT.replace("${'$'}transcription", rawText)
                val body = JSONObject().apply {
                    put("model", model)
                    put("max_tokens", 4096)
                    put("messages", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("content", JSONArray().put(JSONObject().apply {
                            put("type", "text")
                            put("text", prompt)
                        }))
                    }))
                }
                val request = Request.Builder()
                    .url("https://api.anthropic.com/v1/messages")
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                http.newCall(request).execute().use { resp ->
                    val respBody = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(Exception("Claude error ${resp.code}: $respBody"))
                    }
                    val json = JSONObject(respBody)
                    val text = json.getJSONArray("content").getJSONObject(0).getString("text")
                    val usage = json.optJSONObject("usage")
                    val promptTokens = usage?.optInt("input_tokens") ?: 0
                    val completionTokens = usage?.optInt("output_tokens") ?: 0
                    val base = parseExtractionJson(text)
                    Result.success(base.copy(promptTokens = promptTokens, completionTokens = completionTokens))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
