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
 * Google Gemini (generateContent) vision client.
 * Free-tier friendly: gemini-1.5-flash / gemini-2.0-flash have generous free quotas.
 */
class GeminiClient(
    private val model: String = "gemini-2.5-flash"
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
                    put("contents", JSONArray().put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", EXTRACTION_PROMPT))
                            put(JSONObject().put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            }))
                        })
                    }))
                    put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
                }
                android.util.Log.d("GeminiClient", "request payload: " + body.toString())

                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                http.newCall(request).execute().use { resp ->
                    val respBody = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(Exception("Gemini error ${resp.code}: $respBody"))
                    }
                    val json = JSONObject(respBody)
                    val text = json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                    val usage = json.optJSONObject("usageMetadata")
                    val promptTokens = usage?.optInt("promptTokenCount") ?: 0
                    val completionTokens = usage?.optInt("candidatesTokenCount") ?: 0
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
                    put("contents", JSONArray().put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                            if (bitmap != null) {
                                val base64Image = bitmapToBase64Jpeg(bitmap)
                                put(JSONObject().put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                }))
                            }
                        })
                    }))
                    put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
                }
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                http.newCall(request).execute().use { resp ->
                    val respBody = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(Exception("Gemini error ${resp.code}: $respBody"))
                    }
                    val json = JSONObject(respBody)
                    val text = json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                    val usage = json.optJSONObject("usageMetadata")
                    val promptTokens = usage?.optInt("promptTokenCount") ?: 0
                    val completionTokens = usage?.optInt("candidatesTokenCount") ?: 0
                    val base = parseExtractionJson(text)
                    Result.success(base.copy(promptTokens = promptTokens, completionTokens = completionTokens))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
