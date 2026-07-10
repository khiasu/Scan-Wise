package com.khiasu.docscanai.data

data class ParsedQuestionData(
    val type: String,
    val marks: String,
    val question: String,
    val options: Map<String, String>,
    val answerKey: String,
    val explanation: String,
    val paraphrasedQuestion: String = "",
    val answerText: String = "",
    val hint: String = ""
)

fun parseQuestionValue(value: String): ParsedQuestionData {
    var type = "MCQ"
    var marks = "Not specified"
    val options = mutableMapOf<String, String>()

    val lines = value.split("\n")
    var currentSection = ""
    val questionBuilder = StringBuilder()
    val explanationBuilder = StringBuilder()
    val answerKeyBuilder = StringBuilder()
    val paraphrasedBuilder = StringBuilder()
    val answerTextBuilder = StringBuilder()
    val hintBuilder = StringBuilder()

    for (line in lines) {
        val trimmed = line.trim()
        val lower = trimmed.lowercase()
        if (lower.startsWith("type:")) {
            type = trimmed.substring("type:".length).trim().removeSurrounding("[", "]")
        } else if (lower.startsWith("marks:")) {
            marks = trimmed.substring("marks:".length).trim().removeSurrounding("[", "]")
        } else if (lower.startsWith("question:")) {
            currentSection = "QUESTION"
            questionBuilder.append(trimmed.substring("question:".length).trim())
        } else if (lower.startsWith("paraphrased:") || lower.startsWith("paraphrasedquestion:")) {
            currentSection = "PARAPHRASED"
            paraphrasedBuilder.append(trimmed.substringAfter(":").trim())
        } else if (lower.startsWith("options:")) {
            currentSection = "OPTIONS"
        } else if (lower.startsWith("answer key:")) {
            currentSection = "ANSWER"
            answerKeyBuilder.append(trimmed.substring("answer key:".length).trim())
        } else if (lower.startsWith("answer text:")) {
            currentSection = "ANSWERTEXT"
            answerTextBuilder.append(trimmed.substring("answer text:".length).trim())
        } else if (lower.startsWith("explanation:")) {
            currentSection = "EXPLANATION"
            explanationBuilder.append(trimmed.substring("explanation:".length).trim())
        } else if (lower.startsWith("hint:") || lower.startsWith("hints:")) {
            currentSection = "HINT"
            hintBuilder.append(trimmed.substringAfter(":").trim())
        } else {
            when (currentSection) {
                "QUESTION" -> {
                    if (questionBuilder.isNotEmpty()) questionBuilder.append("\n")
                    questionBuilder.append(line)
                }
                "PARAPHRASED" -> {
                    if (paraphrasedBuilder.isNotEmpty()) paraphrasedBuilder.append("\n")
                    paraphrasedBuilder.append(line)
                }
                "OPTIONS" -> {
                    val optMatch = "^\\s*([a-d])\\s*[\\).\\-]\\s*(.*)$".toRegex(RegexOption.IGNORE_CASE).find(line)
                    if (optMatch != null) {
                        val letter = optMatch.groupValues[1].lowercase()
                        val text = optMatch.groupValues[2].trim()
                        options[letter] = text
                    } else if (line.isNotBlank()) {
                        val lastKey = options.keys.lastOrNull()
                        if (lastKey != null) {
                            options[lastKey] = (options[lastKey] + "\n" + trimmed).trim()
                        }
                    }
                }
                "ANSWER" -> {
                    if (answerKeyBuilder.isNotEmpty()) answerKeyBuilder.append("\n")
                    answerKeyBuilder.append(line)
                }
                "ANSWERTEXT" -> {
                    if (answerTextBuilder.isNotEmpty()) answerTextBuilder.append("\n")
                    answerTextBuilder.append(line)
                }
                "EXPLANATION" -> {
                    if (explanationBuilder.isNotEmpty()) explanationBuilder.append("\n")
                    explanationBuilder.append(line)
                }
                "HINT" -> {
                    if (hintBuilder.isNotEmpty()) hintBuilder.append("\n")
                    hintBuilder.append(line)
                }
                else -> {
                    if (questionBuilder.isNotEmpty()) questionBuilder.append("\n")
                    questionBuilder.append(line)
                }
            }
        }
    }

    val finalQuestion = questionBuilder.toString().trim()
    val finalAnswer = answerKeyBuilder.toString().trim()
    val finalExplanation = explanationBuilder.toString().trim()
    val finalParaphrased = paraphrasedBuilder.toString().trim()
    val finalAnswerText = answerTextBuilder.toString().trim()
    val finalHint = hintBuilder.toString().trim()

    var finalType = type
    if (finalType.equals("MCQ", ignoreCase = true) && options.isEmpty()) {
        finalType = "Subjective"
    }

    return ParsedQuestionData(
        type = finalType,
        marks = marks,
        question = finalQuestion.ifEmpty { value.trim() },
        options = options,
        answerKey = finalAnswer,
        explanation = finalExplanation,
        paraphrasedQuestion = finalParaphrased,
        answerText = finalAnswerText,
        hint = finalHint
    )
}

fun serializeQuestionValue(data: ParsedQuestionData): String {
    val sb = StringBuilder()
    sb.append("Type: ${data.type}\n")
    sb.append("Marks: ${data.marks}\n")
    sb.append("Question: ${data.question}\n")
    sb.append("Paraphrased: ${data.paraphrasedQuestion}\n")
    if (data.type.equals("MCQ", ignoreCase = true)) {
        sb.append("Options:\n")
        sb.append("  a) ${data.options.getOrDefault("a", "")}\n")
        sb.append("  b) ${data.options.getOrDefault("b", "")}\n")
        sb.append("  c) ${data.options.getOrDefault("c", "")}\n")
        sb.append("  d) ${data.options.getOrDefault("d", "")}\n")
    }
    sb.append("Answer Key: ${data.answerKey}\n")
    sb.append("Answer Text: ${data.answerText}\n")
    sb.append("Explanation: ${data.explanation}\n")
    sb.append("Hint: ${data.hint}")
    return sb.toString()
}
