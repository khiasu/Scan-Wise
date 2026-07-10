package com.khiasu.docscanai.worker

import android.content.Context
import androidx.work.*
import com.khiasu.docscanai.data.AppDatabase
import com.khiasu.docscanai.network.AiClientFactory
import com.khiasu.docscanai.network.decodeBitmapFromPath
import com.khiasu.docscanai.prefs.SecurePrefs
import org.json.JSONArray
import org.json.JSONObject

/**
 * Runs entirely in the background (survives the user leaving the scan screen).
 * One worker instance == one page. Enqueued as a chain so pages of the same
 * document process in order, but different documents can run in parallel.
 */
class ProcessPageWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_PAGE_ID = "page_id"

        fun enqueueFor(context: Context, pageId: Long, chainTag: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ProcessPageWorker>()
                .setInputData(workDataOf(KEY_PAGE_ID to pageId))
                .setConstraints(constraints)
                .addTag(chainTag)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }

    override suspend fun doWork(): Result {
        val pageId = inputData.getLong(KEY_PAGE_ID, -1)
        if (pageId == -1L) return Result.failure()

        val db = AppDatabase.get(applicationContext)
        val dao = db.docDao()
        val page = dao.getPage(pageId) ?: return Result.failure()

        if (!SecurePrefs.hasApiKey(applicationContext)) {
            dao.updatePage(page.copy(status = "ERROR", errorMessage = "No API key set in Settings"))
            return Result.failure()
        }

        dao.updatePage(page.copy(status = "PROCESSING"))

        return try {
            val bitmap = decodeBitmapFromPath(page.imagePath)
            val provider = SecurePrefs.getProvider(applicationContext)
            val apiKey = SecurePrefs.getApiKey(applicationContext)
            val client = AiClientFactory.create(provider)

            val result = client.extract(apiKey, bitmap)
            result.fold(
                onSuccess = { extraction ->
                    val fieldsJson = JSONArray().apply {
                        extraction.fields.forEach {
                            put(JSONObject().put("key", it.key).put("value", it.value))
                        }
                    }.toString()
                    dao.updatePage(
                        page.copy(
                            status = "DONE",
                            rawText = extraction.rawText,
                            fieldsJson = fieldsJson,
                            promptTokens = extraction.promptTokens,
                            completionTokens = extraction.completionTokens,
                            providerUsed = provider.name,
                            errorMessage = null
                        )
                    )
                    Result.success()
                },
                onFailure = { e ->
                    dao.updatePage(page.copy(status = "ERROR", errorMessage = e.message ?: "Unknown error"))
                    Result.retry()
                }
            )
        } catch (e: Exception) {
            dao.updatePage(page.copy(status = "ERROR", errorMessage = e.message ?: "Unknown error"))
            Result.failure()
        }
    }
}
