package com.khiasu.docscanai.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.khiasu.docscanai.worker.ProcessPageWorker
import com.khiasu.docscanai.prefs.SecurePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ScanRepository(private val context: Context) {

    private val dao = AppDatabase.get(context).docDao()

    fun observeDocuments() = dao.observeDocuments()
    fun observePages(docId: Long) = dao.observePages(docId)
    suspend fun getDocument(docId: Long) = dao.getDocument(docId)
    suspend fun getPages(docId: Long) = dao.getPages(docId)
    suspend fun deleteDocument(docId: Long) = dao.deleteDocument(docId)
    suspend fun updatePage(page: PageEntity) = dao.updatePage(page)
    suspend fun getAllDonePages() = dao.getAllDonePages()

    /** Called once per photo the user takes/picks - "scan one by one". Appends to an in-progress doc. */
    suspend fun createDocumentFromImages(title: String, imageUris: List<Uri>, sourceType: String): Long =
        withContext(Dispatchers.IO) {
            val doc = DocumentEntity(title = title, sourceType = sourceType)
            val docId = dao.insertDocument(doc)

            val chainTag = "doc_$docId"
            imageUris.forEachIndexed { index, uri ->
                val localPath = copyUriToLocalFile(uri)
                val page = PageEntity(documentId = docId, pageIndex = index, imagePath = localPath)
                val pageId = dao.insertPage(page)
                ProcessPageWorker.enqueueFor(context, pageId, chainTag)
            }
            docId
        }

    /** Imports a PDF, rasterizes every page to a bitmap, and processes each like a scanned image. */
    suspend fun createDocumentFromPdf(title: String, pdfUri: Uri): Long =
        withContext(Dispatchers.IO) {
            val doc = DocumentEntity(title = title, sourceType = "PDF")
            val docId = dao.insertDocument(doc)
            val chainTag = "doc_$docId"

            val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(pdfUri, "r")
                ?: throw IllegalStateException("Could not open PDF")
            PdfRenderer(pfd).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { pdfPage ->
                        val bitmap = Bitmap.createBitmap(
                            pdfPage.width * 2, pdfPage.height * 2, Bitmap.Config.ARGB_8888
                        )
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val localPath = saveBitmapToLocalFile(bitmap)
                        val page = PageEntity(documentId = docId, pageIndex = i, imagePath = localPath)
                        val pageId = dao.insertPage(page)
                        ProcessPageWorker.enqueueFor(context, pageId, chainTag)
                    }
                }
            }
            docId
        }

    /** Lets the user re-run a failed page after fixing their API key/network. */
    suspend fun retryPage(pageId: Long) {
        ProcessPageWorker.enqueueFor(context, pageId, "retry_$pageId")
    }

    /** Solves the questions on a page using its raw text content and optional page image bitmap. */
    suspend fun solvePage(pageId: Long): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val page = dao.getPage(pageId) ?: return@withContext Result.failure(Exception("Page not found"))
            val rawText = page.rawText.orEmpty()
            if (rawText.isBlank()) {
                return@withContext Result.failure(Exception("Page raw text is empty. Run scan first."))
            }

            // Load original page bitmap (if it exists) for multimodal analysis/figure cropping
            val pageBitmap: Bitmap? = try {
                if (!page.imagePath.isNullOrBlank()) {
                    android.graphics.BitmapFactory.decodeFile(page.imagePath)
                } else null
            } catch (e: Exception) {
                null
            }
            
            val provider = SecurePrefs.getProvider(context)
            val apiKey = SecurePrefs.getApiKey(context)
            val client = com.khiasu.docscanai.network.AiClientFactory.create(provider)
            
            val apiResult = client.solve(apiKey, rawText, pageBitmap)
            apiResult.fold(
                onSuccess = { extraction ->
                    val processedFields = mutableListOf<com.khiasu.docscanai.network.ExtractedField>()

                    extraction.fields.forEachIndexed { index, field ->
                        var parsed = parseQuestionValue(field.value)
                        
                        // Crop figure if box is found
                        var savedFigurePath = ""
                        val boxStr = parsed.figureBox
                        if (boxStr.isNotEmpty() && !boxStr.equals("None", ignoreCase = true) && pageBitmap != null) {
                            val ints = parseFigureBox(boxStr)
                            if (ints != null) {
                                val ymin = ints[0]
                                val xmin = ints[1]
                                val ymax = ints[2]
                                val xmax = ints[3]
                                val width = pageBitmap.width
                                val height = pageBitmap.height
                                val left = (xmin * width / 1000).coerceIn(0, width - 1)
                                val top = (ymin * height / 1000).coerceIn(0, height - 1)
                                val right = (xmax * width / 1000).coerceIn(left + 1, width)
                                val bottom = (ymax * height / 1000).coerceIn(top + 1, height)
                                val cropW = right - left
                                val cropH = bottom - top
                                if (cropW > 0 && cropH > 0) {
                                    try {
                                        val cropped = Bitmap.createBitmap(pageBitmap, left, top, cropW, cropH)
                                        val figuresDir = File(context.cacheDir, "figures").apply { mkdirs() }
                                        val figureFile = File(figuresDir, "fig_${pageId}_${index}_${System.currentTimeMillis()}.jpg")
                                        FileOutputStream(figureFile).use { out ->
                                            cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                        }
                                        savedFigurePath = figureFile.absolutePath
                                    } catch (e: Exception) {
                                        android.util.Log.e("ScanRepository", "Failed to crop figure: ${e.message}")
                                    }
                                }
                            }
                        }

                        // If figure image was successfully cropped and saved, update parsed data
                        if (savedFigurePath.isNotEmpty()) {
                            parsed = parsed.copy(figureImagePath = savedFigurePath)
                        }
                        
                        // Re-serialize the updated parsed data back to field value
                        val updatedValue = serializeQuestionValue(parsed)
                        processedFields.add(com.khiasu.docscanai.network.ExtractedField(field.key, updatedValue))
                    }

                    val fieldsJson = org.json.JSONArray().apply {
                        processedFields.forEach {
                            put(org.json.JSONObject().put("key", it.key).put("value", it.value))
                        }
                    }.toString()
                    
                    // Format questions and answers into rawText so users can edit them in the Page Editor
                    val formattedBuilder = java.lang.StringBuilder()
                    processedFields.forEach { field ->
                        val parsed = parseQuestionValue(field.value)
                        formattedBuilder.append("=== ").append(field.key).append(" ===\n")
                        formattedBuilder.append("Question: ").append(parsed.question).append("\n")
                        if (parsed.type.equals("MCQ", ignoreCase = true)) {
                            parsed.options.forEach { (k, v) ->
                                formattedBuilder.append("  ").append(k).append(") ").append(v).append("\n")
                            }
                            formattedBuilder.append("Answer Key: ").append(parsed.answerKey.uppercase())
                            if (parsed.answerText.isNotEmpty()) {
                                formattedBuilder.append(" (").append(parsed.answerText).append(")")
                            }
                            formattedBuilder.append("\n")
                        } else {
                            formattedBuilder.append("Answer: ").append(parsed.answerKey).append("\n")
                        }
                        if (parsed.explanation.isNotEmpty()) {
                            formattedBuilder.append("Explanation: ").append(parsed.explanation).append("\n")
                        }
                        formattedBuilder.append("\n")
                    }
                    val finalRawText = formattedBuilder.toString().trim()
                    
                    dao.updatePage(
                        page.copy(
                            fieldsJson = fieldsJson,
                            rawText = if (finalRawText.isNotEmpty()) finalRawText else rawText,
                            promptTokens = page.promptTokens + extraction.promptTokens,
                            completionTokens = page.completionTokens + extraction.completionTokens,
                            providerUsed = provider.name
                        )
                    )
                    Result.success(Unit)
                },
                onFailure = { e ->
                    Result.failure(e)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseFigureBox(boxStr: String): List<Int>? {
        val cleaned = boxStr.replace("[", "").replace("]", "").replace(" ", "")
        val parts = cleaned.split(",")
        if (parts.size == 4) {
            val ints = parts.mapNotNull { it.toIntOrNull() }
            if (ints.size == 4) return ints
        }
        return null
    }

    suspend fun deletePage(pageId: Long) = withContext(Dispatchers.IO) {
        dao.deletePage(pageId)
    }

    private fun copyUriToLocalFile(uri: Uri): String {
        val dir = File(context.cacheDir, "pages").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        return file.absolutePath
    }

    private fun saveBitmapToLocalFile(bitmap: Bitmap): String {
        val dir = File(context.cacheDir, "pages").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
        return file.absolutePath
    }
}
