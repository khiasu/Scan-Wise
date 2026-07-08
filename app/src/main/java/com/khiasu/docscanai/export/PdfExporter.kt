package com.khiasu.docscanai.export

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream

/** Builds a simple readable PDF report using Android's built-in canvas-based PdfDocument. */
object PdfExporter {

    private const val PAGE_WIDTH = 595  // A4 at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    fun buildMerged(docTitle: String, pages: List<PageResult>): ByteArray {
        val pdf = PdfDocument()
        pages.forEachIndexed { idx, page ->
            renderPage(pdf, "$docTitle - Page ${page.pageIndex + 1}", page)
        }
        if (pages.isEmpty()) renderPage(pdf, docTitle, PageResult(0, "(no pages)", emptyList()))
        return pdf.toBytes()
    }

    fun buildSinglePage(docTitle: String, page: PageResult): ByteArray {
        val pdf = PdfDocument()
        renderPage(pdf, "$docTitle - Page ${page.pageIndex + 1}", page)
        return pdf.toBytes()
    }

    private fun renderPage(pdf: PdfDocument, heading: String, page: PageResult) {
        val titlePaint = Paint().apply { textSize = 16f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f }
        val labelPaint = Paint().apply { textSize = 11f; isFakeBoldText = true }

        var pageNumber = pdf.pages.size + 1
        var pdfPage = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = pdfPage.canvas
        var y = MARGIN + 10

        canvas.drawText(heading, MARGIN, y, titlePaint)
        y += 24

        if (page.fields.isNotEmpty()) {
            canvas.drawText("Extracted fields:", MARGIN, y, labelPaint)
            y += 18
            page.fields.forEach { f ->
                val line = "${f.key}: ${f.value}"
                val wrapped = wrapText(line, bodyPaint, PAGE_WIDTH - 2 * MARGIN)
                wrapped.forEach { l ->
                    if (y > PAGE_HEIGHT - MARGIN) {
                        pdf.finishPage(pdfPage)
                        pageNumber++
                        pdfPage = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                        canvas = pdfPage.canvas
                        y = MARGIN
                    }
                    canvas.drawText(l, MARGIN, y, bodyPaint)
                    y += 16
                }
            }
            y += 10
        }

        canvas.drawText("Full text:", MARGIN, y, labelPaint)
        y += 18
        wrapText(page.rawText, bodyPaint, PAGE_WIDTH - 2 * MARGIN).forEach { line ->
            if (y > PAGE_HEIGHT - MARGIN) {
                pdf.finishPage(pdfPage)
                pageNumber++
                pdfPage = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = pdfPage.canvas
                y = MARGIN
            }
            canvas.drawText(line, MARGIN, y, bodyPaint)
            y += 16
        }

        pdf.finishPage(pdfPage)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        text.split("\n").forEach { rawLine ->
            var line = ""
            rawLine.split(" ").forEach { word ->
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) > maxWidth) {
                    if (line.isNotEmpty()) result.add(line)
                    line = word
                } else {
                    line = candidate
                }
            }
            result.add(line)
        }
        return result
    }

    private fun PdfDocument.toBytes(): ByteArray {
        val out = ByteArrayOutputStream()
        writeTo(out)
        close()
        return out.toByteArray()
    }
}
