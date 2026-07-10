package com.khiasu.docscanai.network

/**
 * Helper to calculate the token cost in Indian Rupees (INR) for various AI providers.
 * Rates are based on standard public API pricing (per 1,000,000 tokens) converted to INR
 * at a static exchange rate of ₹83.5 INR/USD.
 */
object ApiPricing {
    // USD rates per 1,000,000 tokens (Input / Output)
    private const val RATE_GEMINI_INPUT = 0.075  // Gemini 1.5 Flash
    private const val RATE_GEMINI_OUTPUT = 0.30

    private const val RATE_OPENAI_INPUT = 0.150  // gpt-4o-mini
    private const val RATE_OPENAI_OUTPUT = 0.600

    private const val RATE_CLAUDE_INPUT = 3.00   // Claude 3.5 Sonnet
    private const val RATE_CLAUDE_OUTPUT = 15.00

    private const val RATE_GROQ_INPUT = 0.0      // Llama free tier on Groq
    private const val RATE_GROQ_OUTPUT = 0.0

    private const val USD_TO_INR = 83.5

    fun calculateCostInr(provider: String, promptTokens: Int, completionTokens: Int): Double {
        val (inputRate, outputRate) = when (provider.uppercase()) {
            "GEMINI" -> RATE_GEMINI_INPUT to RATE_GEMINI_OUTPUT
            "OPENAI" -> RATE_OPENAI_INPUT to RATE_OPENAI_OUTPUT
            "CLAUDE" -> RATE_CLAUDE_INPUT to RATE_CLAUDE_OUTPUT
            else -> RATE_GROQ_INPUT to RATE_GROQ_OUTPUT
        }
        val inputCostUsd = (promptTokens.toDouble() / 1_000_000.0) * inputRate
        val outputCostUsd = (completionTokens.toDouble() / 1_000_000.0) * outputRate
        return (inputCostUsd + outputCostUsd) * USD_TO_INR
    }
}
