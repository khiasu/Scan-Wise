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

/**
 * Groq cloud AI client using their OpenAI-compatible API.
 * Free tier: 30 RPM, 14,400 RPD — very generous for document scanning.
 * Uses Llama 3.2 Vision (90B) for high-quality text extraction + analysis.
 */
class GroqClient(
    private val model: String = "meta-llama/llama-4-scout-17b-16e-instruct"
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
                    put("messages", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("content", JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "text")
                                put("text", EXTRACTION_PROMPT)
                            })
                            put(JSONObject().apply {
                                put("type", "image_url")
                                put("image_url", JSONObject().apply {
                                    put("url", "data:image/jpeg;base64,$base64Image")
                                })
                            })
                        })
                    }))
                    put("response_format", JSONObject().put("type", "json_object"))
                    put("max_tokens", 4096)
                }

                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                http.newCall(request).execute().use { resp ->
                    val respBody = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(Exception("Groq error ${resp.code}: $respBody"))
                    }
                    val json = JSONObject(respBody)
                    val text = json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    Result.success(parseExtractionJson(text))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
