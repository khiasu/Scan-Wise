package com.khiasu.docscanai.prefs

import android.content.Context

object ThemePrefs {
    enum class ThemeMode { SYSTEM, LIGHT, DARK }

    private const val PREFS = "theme_prefs"
    private const val KEY = "theme_mode"

    fun get(context: Context): ThemeMode {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun set(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, mode.name).apply()
    }
}
