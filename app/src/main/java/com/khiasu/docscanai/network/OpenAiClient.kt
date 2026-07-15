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

/** OpenAI chat completions vision client (gpt-4o-mini by default - cheapest vision model). */
class OpenAiClient(
    private val model: String = "gpt-4o-mini"
) : AiClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun extract(apiKey: String, bitmap: Bitmap): Result<ExtractionResult> =
        withContext(Dispatchers.IO) {
            try {
                val base64Image = bitmapToBase64Jpeg(bitmap)
                val dataUrl = "data:image/jpeg;base64,$base64Image"

                val body = JSONObject().apply {
                    put("model", model)
                    put("response_format", JSONObject().put("type", "json_object"))
                    put("messages", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("content", JSONArray().apply {
                            put(JSONObject().put("type", "text").put("text", EXTRACTION_PROMPT))
                            put(JSONObject().put("type", "image_url").put(
                                "image_url", JSONObject().put("url", dataUrl)
                            ))
                        })
                    }))
                }

                val request = Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                http.newCall(request).execute().use { resp ->
                    val respBody = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(Exception("OpenAI error ${resp.code}: $respBody"))
                    }
                    val json = JSONObject(respBody)
                    val text = json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    val usage = json.optJSONObject("usage")
                    val promptTokens = usage?.optInt("prompt_tokens") ?: 0
                    val completionTokens = usage?.optInt("completion_tokens") ?: 0
                    val base = parseExtractionJson(text)
                    Result.success(base.copy(promptTokens = promptTokens, completionTokens = completionTokens))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun solve(apiKey: String, rawText: String, bitmap: Bitmap?): Result<ExtractionResult> =
        withContext(Dispatchers.IO) {
            try {
                val prompt = SOLVE_PROMPT.replace("${'$'}transcription", rawText)
                val body = JSONObject().apply {
                    put("model", model)
                    put("response_format", JSONObject().put("type", "json_object"))
                    put("messages", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("content", if (bitmap != null) {
                            val base64Image = bitmapToBase64Jpeg(bitmap)
                            val dataUrl = "data:image/jpeg;base64,$base64Image"
                            JSONArray().apply {
                                put(JSONObject().put("type", "text").put("text", prompt))
                                put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", dataUrl)))
                            }
                        } else {
                            prompt
                        })
                    }))
                }
                val request = Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                http.newCall(request).execute().use { resp ->
                    val respBody = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(Exception("OpenAI error ${resp.code}: $respBody"))
                    }
                    val json = JSONObject(respBody)
                    val text = json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    val usage = json.optJSONObject("usage")
                    val promptTokens = usage?.optInt("prompt_tokens") ?: 0
                    val completionTokens = usage?.optInt("completion_tokens") ?: 0
                    val base = parseExtractionJson(text)
                    Result.success(base.copy(promptTokens = promptTokens, completionTokens = completionTokens))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
