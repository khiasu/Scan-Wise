package com.khiasu.docscanai.network

import com.khiasu.docscanai.prefs.SecurePrefs

object AiClientFactory {
    fun create(provider: SecurePrefs.Provider): AiClient = when (provider) {
        SecurePrefs.Provider.OFFLINE -> MlKitOcrClient()
        SecurePrefs.Provider.GROQ    -> GroqClient()
        SecurePrefs.Provider.GEMINI  -> GeminiClient()
        SecurePrefs.Provider.OPENAI  -> OpenAiClient()
        SecurePrefs.Provider.CLAUDE  -> ClaudeClient()
    }
}
