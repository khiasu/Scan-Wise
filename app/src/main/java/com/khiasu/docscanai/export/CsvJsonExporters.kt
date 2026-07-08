package com.khiasu.docscanai.export

import org.json.JSONArray
import org.json.JSONObject

object CsvExporter {
    /** Merged: one CSV with every field from every page, tagged by page number. */
    fun buildMerged(docTitle: String, pages: List<PageResult>): ByteArray {
        val sb = StringBuilder()
        sb.append("page,key,value\n")
        pages.forEach { page ->
            if (page.fields.isEmpty()) {
                sb.append("${page.pageIndex + 1},raw_text,${escape(page.rawText)}\n")
            } else {
                page.fields.forEach { f ->
                    sb.append("${page.pageIndex + 1},${escape(f.key)},${escape(f.value)}\n")
                }
            }
        }
        return sb.toString().toByteArray()
    }

    /** Per-page: one CSV for a single page's fields. */
    fun buildSinglePage(page: PageResult): ByteArray {
        val sb = StringBuilder()
        sb.append("key,value\n")
        if (page.fields.isEmpty()) {
            sb.append("raw_text,${escape(page.rawText)}\n")
        } else {
            page.fields.forEach { f -> sb.append("${escape(f.key)},${escape(f.value)}\n") }
        }
        return sb.toString().toByteArray()
    }

    private fun escape(value: String): String {
        val cleaned = value.replace("\"", "\"\"").replace("\n", " ")
        return "\"$cleaned\""
    }
}

object JsonExporter {
    fun buildMerged(docTitle: String, pages: List<PageResult>): ByteArray {
        val root = JSONObject()
        root.put("document", docTitle)
        val pagesArr = JSONArray()
        pages.forEach { page ->
            val pageObj = JSONObject()
            pageObj.put("page", page.pageIndex + 1)
            pageObj.put("raw_text", page.rawText)
            val fieldsArr = JSONArray()
            page.fields.forEach { f -> fieldsArr.put(JSONObject().put("key", f.key).put("value", f.value)) }
            pageObj.put("fields", fieldsArr)
            pagesArr.put(pageObj)
        }
        root.put("pages", pagesArr)
        return root.toString(2).toByteArray()
    }

    fun buildSinglePage(page: PageResult): ByteArray {
        val obj = JSONObject()
        obj.put("page", page.pageIndex + 1)
        obj.put("raw_text", page.rawText)
        val fieldsArr = JSONArray()
        page.fields.forEach { f -> fieldsArr.put(JSONObject().put("key", f.key).put("value", f.value)) }
        obj.put("fields", fieldsArr)
        return obj.toString(2).toByteArray()
    }
}
