package com.khiasu.docscanai.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

data class ExtractedField(val key: String, val value: String)

data class ExtractionResult(
    val rawText: String,
    val fields: List<ExtractedField>,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0
)

/** Prompt used for initial rapid image OCR transcription. */
const val EXTRACTION_PROMPT = """
You are a high-accuracy document transcription assistant.
Transcribe ALL visible text on this page exactly as it appears. Preserve line breaks, paragraphs, questions, options, headings, and lists.
When you encounter a diagram, figure, graph, chart, or image embedded in the page, insert a marker at that position in the format: [FIGURE: brief description of the visual content]
Do not add commentary or summary beyond figure markers - just return the raw text.
Respond with ONLY a valid JSON object in this format (no markdown code block fences like ```json, no extra text):
{
  "raw_text": "[Transcribed raw text of the page with [FIGURE: ...] markers]",
  "fields": []
}
"""

/** Prompt used to solve questions, build MCQ details, correct answers, explanations, and hints from page text. */
const val SOLVE_PROMPT = """
You are a Question Bank digitizer. Analyze the page image (if available) and the transcription to extract all questions into structured details.
For each question, extract:
- key: "Question X" (e.g. "Question 1", "Question 2")
- value: Format it exactly as follows, using newlines (\n) to separate the parts. Keep the explanation and paraphrasing extremely brief (max 1 short sentence). Omit options if it is a subjective question:
Type: [MCQ or Subjective]
Marks: [Allocated marks/points if shown, e.g. 5 marks, or "Not specified"]
Question: [exact question text]
Paraphrased: [clear, rephrased version (max 1 sentence)]
Options: (Include only if MCQ)
  a) [Option A]
  b) [Option B]
  c) [Option C]
  d) [Option D]
Answer Key: [Correct Option letter (e.g. a, b, c, d) or a brief model answer/rubric if subjective]
Answer Text: [The exact text of the correct option]
Explanation: [Explain why the answer is correct in max 1 sentence]
Hint: [A small tip or clue to help solve it (max 1 sentence)]
Figure: [If there is an associated diagram/figure in the image near this question, describe it here, else leave blank]
Figure Box: [If there is a diagram/figure/chart/table image associated with this question, specify its bounding box in ymin,xmin,ymax,xmax format normalized on 0 to 1000 scale relative to the height and width of the image, e.g. [200,150,450,850]. Otherwise write None]

Input Text:
${'$'}transcription

Respond with ONLY a valid JSON object in this format (no markdown code block fences like ```json, no extra text):
{
  "fields": [{"key": "Question 1", "value": "..."}]
}
"""

interface AiClient {
    /** Sends one page image to the provider and returns the parsed extraction. */
    suspend fun extract(apiKey: String, bitmap: Bitmap): Result<ExtractionResult>

    /** Solves and parses structured question fields from the page's transcribed raw text and optional bitmap. */
    suspend fun solve(apiKey: String, rawText: String, bitmap: Bitmap? = null): Result<ExtractionResult>
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
