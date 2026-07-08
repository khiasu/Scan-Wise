package com.khiasu.docscanai.export

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Builds a minimal but genuinely valid .docx by hand-writing the OOXML parts
 * and zipping them. Deliberately avoids Apache POI, which drags in java.awt
 * dependencies that don't exist on Android.
 */
object DocxExporter {

    fun buildMerged(docTitle: String, pages: List<PageResult>): ByteArray {
        val body = StringBuilder()
        body.append(heading(docTitle, 32))
        pages.forEach { page ->
            body.append(heading("Page ${page.pageIndex + 1}", 26))
            if (page.fields.isNotEmpty()) {
                body.append(heading("Extracted fields", 22))
                page.fields.forEach { f -> body.append(paragraph("${f.key}: ${f.value}", bold = false)) }
            }
            body.append(heading("Full text", 22))
            page.rawText.split("\n").forEach { line -> body.append(paragraph(line)) }
        }
        return buildDocx(body.toString())
    }

    fun buildSinglePage(docTitle: String, page: PageResult): ByteArray {
        val body = StringBuilder()
        body.append(heading("$docTitle - Page ${page.pageIndex + 1}", 32))
        if (page.fields.isNotEmpty()) {
            body.append(heading("Extracted fields", 22))
            page.fields.forEach { f -> body.append(paragraph("${f.key}: ${f.value}", bold = false)) }
        }
        body.append(heading("Full text", 22))
        page.rawText.split("\n").forEach { line -> body.append(paragraph(line)) }
        return buildDocx(body.toString())
    }

    private fun heading(text: String, sizeHalfPoints: Int) =
        """<w:p><w:pPr><w:rPr><w:b/><w:sz w:val="$sizeHalfPoints"/></w:rPr></w:pPr>""" +
            """<w:r><w:rPr><w:b/><w:sz w:val="$sizeHalfPoints"/></w:rPr><w:t xml:space="preserve">${escape(text)}</w:t></w:r></w:p>"""

    private fun paragraph(text: String, bold: Boolean = false) =
        if (bold) """<w:p><w:r><w:rPr><w:b/></w:rPr><w:t xml:space="preserve">${escape(text)}</w:t></w:r></w:p>"""
        else """<w:p><w:r><w:t xml:space="preserve">${escape(text)}</w:t></w:r></w:p>"""

    private fun escape(text: String) = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private fun buildDocx(bodyXml: String): ByteArray {
        val documentXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
            <w:body>$bodyXml
            <w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1417" w:right="1417" w:bottom="1417" w:left="1417"/></w:sectPr>
            </w:body></w:document>""".trimIndent()

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
            <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
            <Default Extension="xml" ContentType="application/xml"/>
            <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>""".trimIndent()

        val rootRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
            </Relationships>""".trimIndent()

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun writeEntry(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
            writeEntry("[Content_Types].xml", contentTypes)
            writeEntry("_rels/.rels", rootRels)
            writeEntry("word/document.xml", documentXml)
        }
        return out.toByteArray()
    }
}
