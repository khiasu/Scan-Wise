package com.khiasu.docscanai.prefs

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores the user's own AI provider + API key locally on-device using
 * AES256-GCM encrypted SharedPreferences (Android Keystore backed).
 *
 * Nothing here is ever transmitted anywhere except directly to the chosen
 * provider's own API endpoint when a scan is processed. There is no
 * account system, no analytics, no backend of ours involved at all -
 * the app is a thin, anonymous pass-through to whichever API the user
 * configures.
 */
object SecurePrefs {

    enum class Provider {
        OFFLINE,  // On-device ML Kit OCR — no key, no internet
        GROQ,     // Groq free tier — Llama 3.2 Vision
        GEMINI,   // Google Gemini
        OPENAI,   // OpenAI GPT-4 Vision
        CLAUDE    // Anthropic Claude
    }

    private const val FILE_NAME = "secure_prefs"
    private const val KEY_PROVIDER = "provider"
    private const val KEY_API_KEY = "api_key"

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(context: Context, provider: Provider, apiKey: String) {
        prefs(context).edit()
            .putString(KEY_PROVIDER, provider.name)
            .putString(KEY_API_KEY, apiKey.trim())
            .apply()
    }

    fun getProvider(context: Context): Provider {
        val name = prefs(context).getString(KEY_PROVIDER, Provider.OFFLINE.name)
        return runCatching { Provider.valueOf(name ?: Provider.OFFLINE.name) }.getOrDefault(Provider.OFFLINE)
    }

    fun getApiKey(context: Context): String =
        prefs(context).getString(KEY_API_KEY, "") ?: ""

    /** Returns true if the current provider can operate (offline always can, cloud needs a key). */
    fun isReady(context: Context): Boolean {
        val provider = getProvider(context)
        return provider == Provider.OFFLINE || getApiKey(context).isNotBlank()
    }

    /** Legacy compat — true if a cloud API key is configured. */
    fun hasApiKey(context: Context): Boolean = getApiKey(context).isNotBlank()

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
