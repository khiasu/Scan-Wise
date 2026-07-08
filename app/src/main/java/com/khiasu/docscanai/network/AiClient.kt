package com.khiasu.docscanai.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

data class ExtractedField(val key: String, val value: String)

data class ExtractionResult(
    val rawText: String,
    val fields: List<ExtractedField>
)

/** The single prompt sent to every provider - keeps behavior consistent across them. */
const val EXTRACTION_PROMPT = """
You are a document scanning assistant. Look at this image of a page and do two things:
1. Transcribe ALL visible text exactly as it appears (preserve line breaks where meaningful).
2. Separately, pull out any clear key-value data points you can find (e.g. dates, names,
   totals, invoice numbers, headers, labels and their values). If none exist, return an
   empty list for fields.

Respond with ONLY valid JSON, no markdown fences, no commentary, in exactly this shape:
{"raw_text": "...", "fields": [{"key": "...", "value": "..."}]}
"""

interface AiClient {
    /** Sends one page image to the provider and returns the parsed extraction. */
    suspend fun extract(apiKey: String, bitmap: Bitmap): Result<ExtractionResult>
}

fun bitmapToBase64Jpeg(bitmap: Bitmap, quality: Int = 85): String {
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}

/** Best-effort JSON parse of the model's response into our ExtractionResult. */
fun parseExtractionJson(text: String): ExtractionResult {
    // Models occasionally wrap JSON in ```json fences despite instructions - strip them.
    val cleaned = text.trim()
        .removePrefix("```json").removePrefix("```")
        .removeSuffix("```").trim()
    return try {
        val obj = org.json.JSONObject(cleaned)
        val rawText = obj.optString("raw_text", "")
        val fieldsArr = obj.optJSONArray("fields")
        val fields = mutableListOf<ExtractedField>()
        if (fieldsArr != null) {
            for (i in 0 until fieldsArr.length()) {
                val f = fieldsArr.getJSONObject(i)
                fields.add(ExtractedField(f.optString("key"), f.optString("value")))
            }
        }
        ExtractionResult(rawText, fields)
    } catch (e: Exception) {
        // If the model didn't return clean JSON, fall back to treating the whole
        // response as raw text so the user still gets something useful.
        ExtractionResult(rawText = cleaned, fields = emptyList())
    }
}

fun decodeBitmapFromPath(path: String): Bitmap =
    BitmapFactory.decodeFile(path) ?: throw IllegalStateException("Could not decode image at $path")
