package com.khiasu.docscanai.network

import android.graphics.Bitmap
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * On-device OCR using Google ML Kit.
 * Runs completely offline — no API key, no internet, near-instant results.
 * Only extracts raw text (no AI-powered field extraction).
 */
class MlKitOcrClient : AiClient {

    override suspend fun extract(apiKey: String, bitmap: Bitmap): Result<ExtractionResult> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            
            // Custom suspend extension to await Google Task completion
            val visionText = recognizer.process(image).await()

            val rawText = visionText.text
            if (rawText.isBlank()) {
                Result.success(ExtractionResult(rawText = "(No text detected on this page)", fields = emptyList()))
            } else {
                Result.success(ExtractionResult(rawText = rawText, fields = emptyList()))
            }
        } catch (e: Exception) {
            Result.failure(Exception("On-device OCR failed: ${e.message}"))
        }
    }

    // Helper extension to adapt Play Services Tasks to Kotlin Coroutines without extra library dependencies
    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cont.resume(task.result)
            } else {
                cont.resumeWithException(task.exception ?: Exception("Task execution failed"))
            }
        }
    }
}
