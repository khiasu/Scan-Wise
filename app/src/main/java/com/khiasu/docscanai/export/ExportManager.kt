package com.khiasu.docscanai.export

import android.content.Context
import android.net.Uri
import com.khiasu.docscanai.data.DocumentEntity
import com.khiasu.docscanai.data.PageEntity

object ExportManager {

    /** Returns the list of MediaStore Uris written (one for MERGED, one per page for PER_PAGE). */
    fun export(
        context: Context,
        doc: DocumentEntity,
        pages: List<PageEntity>,
        scope: ExportScope,
        format: ExportFormat
    ): List<Uri> {
        val done = pages.doneOnly().map { it.toPageResult() }
        val safeTitle = doc.title.replace(Regex("[^A-Za-z0-9_-]"), "_")

        return if (scope == ExportScope.MERGED) {
            val bytes = when (format) {
                ExportFormat.PDF -> PdfExporter.buildMerged(doc.title, done)
                ExportFormat.DOCX -> DocxExporter.buildMerged(doc.title, done)
                ExportFormat.CSV -> CsvExporter.buildMerged(doc.title, done)
                ExportFormat.JSON -> JsonExporter.buildMerged(doc.title, done)
            }
            val fileName = "${safeTitle}_${System.currentTimeMillis()}.${format.extension}"
            listOf(MediaStoreWriter.writeToDownloads(context, fileName, format.mimeType, bytes))
        } else {
            done.map { page ->
                val bytes = when (format) {
                    ExportFormat.PDF -> PdfExporter.buildSinglePage(doc.title, page)
                    ExportFormat.DOCX -> DocxExporter.buildSinglePage(doc.title, page)
                    ExportFormat.CSV -> CsvExporter.buildSinglePage(page)
                    ExportFormat.JSON -> JsonExporter.buildSinglePage(page)
                }
                val fileName = "${safeTitle}_page${page.pageIndex + 1}_${System.currentTimeMillis()}.${format.extension}"
                MediaStoreWriter.writeToDownloads(context, fileName, format.mimeType, bytes)
            }
        }
    }
}
