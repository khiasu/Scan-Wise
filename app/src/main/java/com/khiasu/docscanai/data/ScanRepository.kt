package com.khiasu.docscanai.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.khiasu.docscanai.worker.ProcessPageWorker
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
