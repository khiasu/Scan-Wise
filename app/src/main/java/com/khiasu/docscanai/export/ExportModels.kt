package com.khiasu.docscanai.export

import com.khiasu.docscanai.data.PageEntity
import org.json.JSONArray

enum class ExportScope { MERGED, PER_PAGE }
enum class ExportFormat(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    CSV("csv", "text/csv"),
    JSON("json", "application/json")
}

data class FieldRow(val key: String, val value: String)

/** One page's parsed result, ready to hand to any exporter. */
data class PageResult(
    val pageIndex: Int,
    val rawText: String,
    val fields: List<FieldRow>
)

fun PageEntity.toPageResult(): PageResult {
    val fields = mutableListOf<FieldRow>()
    fieldsJson?.let { json ->
        runCatching {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                fields.add(FieldRow(obj.optString("key"), obj.optString("value")))
            }
        }
    }
    return PageResult(pageIndex, rawText.orEmpty(), fields)
}

/** Only pages that finished successfully are exportable. */
fun List<PageEntity>.doneOnly(): List<PageEntity> = filter { it.status == "DONE" }
