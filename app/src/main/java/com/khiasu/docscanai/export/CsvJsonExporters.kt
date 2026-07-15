package com.khiasu.docscanai.export

import org.json.JSONArray
import org.json.JSONObject
import com.khiasu.docscanai.data.parseQuestionValue
import com.khiasu.docscanai.data.ParsedQuestionData

object CsvExporter {
    /** Merged: one CSV with every field from every page, split into proper columns. */
    fun buildMerged(docTitle: String, pages: List<PageResult>): ByteArray {
        val sb = StringBuilder()
        sb.append("Page,Question Number,Type,Marks,Question,Paraphrased Question,Option A,Option B,Option C,Option D,Correct Answer,Explanation,Hint,Figure Description\n")
        pages.forEach { page ->
            if (page.fields.isEmpty()) {
                sb.append("${page.pageIndex + 1},Intro,Subjective,Not specified,${escape(page.rawText)},,,,,,,,,\n")
            } else {
                page.fields.forEach { f ->
                    val parsed = parseQuestionValue(f.value)
                    sb.append("${page.pageIndex + 1},")
                    sb.append("${escape(f.key)},")
                    sb.append("${escape(parsed.type)},")
                    sb.append("${escape(parsed.marks)},")
                    sb.append("${escape(parsed.question)},")
                    sb.append("${escape(parsed.paraphrasedQuestion)},")
                    sb.append("${escape(parsed.options.getOrDefault("a", ""))},")
                    sb.append("${escape(parsed.options.getOrDefault("b", ""))},")
                    sb.append("${escape(parsed.options.getOrDefault("c", ""))},")
                    sb.append("${escape(parsed.options.getOrDefault("d", ""))},")
                    sb.append("${escape(parsed.answerKey)},")
                    sb.append("${escape(parsed.explanation)},")
                    sb.append("${escape(parsed.hint)},")
                    sb.append("${escape(parsed.figureDescription)}\n")
                }
            }
        }
        return sb.toString().toByteArray()
    }

    /** Per-page: one CSV for a single page's fields. */
    fun buildSinglePage(page: PageResult): ByteArray {
        val sb = StringBuilder()
        sb.append("Question Number,Type,Marks,Question,Paraphrased Question,Option A,Option B,Option C,Option D,Correct Answer,Explanation,Hint,Figure Description\n")
        if (page.fields.isEmpty()) {
            sb.append("Intro,Subjective,Not specified,${escape(page.rawText)},,,,,,,,,\n")
        } else {
            page.fields.forEach { f ->
                val parsed = parseQuestionValue(f.value)
                sb.append("${escape(f.key)},")
                sb.append("${escape(parsed.type)},")
                sb.append("${escape(parsed.marks)},")
                sb.append("${escape(parsed.question)},")
                sb.append("${escape(parsed.paraphrasedQuestion)},")
                sb.append("${escape(parsed.options.getOrDefault("a", ""))},")
                sb.append("${escape(parsed.options.getOrDefault("b", ""))},")
                sb.append("${escape(parsed.options.getOrDefault("c", ""))},")
                sb.append("${escape(parsed.options.getOrDefault("d", ""))},")
                sb.append("${escape(parsed.answerKey)},")
                sb.append("${escape(parsed.explanation)},")
                sb.append("${escape(parsed.hint)},")
                sb.append("${escape(parsed.figureDescription)}\n")
            }
        }
        return sb.toString().toByteArray()
    }

    private fun escape(value: String): String {
        val cleaned = value.replace("\"", "\"\"")
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
            val questionsArr = JSONArray()
            page.fields.forEach { f ->
                val parsed = parseQuestionValue(f.value)
                val qObj = JSONObject().apply {
                    put("question_number", f.key)
                    put("type", parsed.type)
                    put("marks", parsed.marks)
                    put("question", parsed.question)
                    put("paraphrased_question", parsed.paraphrasedQuestion)
                    put("options", JSONObject().apply {
                        parsed.options.forEach { (k, v) -> put(k, v) }
                    })
                    put("correct_answer", parsed.answerKey)
                    put("answer_text", parsed.answerText)
                    put("explanation", parsed.explanation)
                    put("hint", parsed.hint)
                    put("figure_description", parsed.figureDescription)
                }
                questionsArr.put(qObj)
            }
            pageObj.put("questions", questionsArr)
            pagesArr.put(pageObj)
        }
        root.put("pages", pagesArr)
        return root.toString(2).toByteArray()
    }

    fun buildSinglePage(page: PageResult): ByteArray {
        val pageObj = JSONObject()
        pageObj.put("page", page.pageIndex + 1)
        pageObj.put("raw_text", page.rawText)
        val questionsArr = JSONArray()
        page.fields.forEach { f ->
            val parsed = parseQuestionValue(f.value)
            val qObj = JSONObject().apply {
                put("question_number", f.key)
                put("type", parsed.type)
                put("marks", parsed.marks)
                put("question", parsed.question)
                put("paraphrased_question", parsed.paraphrasedQuestion)
                put("options", JSONObject().apply {
                    parsed.options.forEach { (k, v) -> put(k, v) }
                })
                put("correct_answer", parsed.answerKey)
                put("answer_text", parsed.answerText)
                put("explanation", parsed.explanation)
                put("hint", parsed.hint)
                put("figure_description", parsed.figureDescription)
            }
            questionsArr.put(qObj)
        }
        pageObj.put("questions", questionsArr)
        return pageObj.toString(2).toByteArray()
    }
}
