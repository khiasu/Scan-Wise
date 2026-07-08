package com.khiasu.docscanai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.khiasu.docscanai.prefs.ThemePrefs
import com.khiasu.docscanai.ui.AppRoot
import com.khiasu.docscanai.ui.theme.ScanWiseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentThemeMode = ThemePrefs.get(this)
        themeUpdater = { currentThemeMode = it }
        setContent {
            val systemDark = isSystemInDarkTheme()
            val darkTheme = remember {
                derivedStateOf {
                    when (currentThemeMode) {
                        ThemePrefs.ThemeMode.SYSTEM -> systemDark
                        ThemePrefs.ThemeMode.LIGHT -> false
                        ThemePrefs.ThemeMode.DARK -> true
                    }
                }
            }

            ScanWiseTheme(darkTheme = darkTheme.value) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    AppRoot()
                }
            }
        }
    }

    companion object {
        var currentThemeMode by mutableStateOf(ThemePrefs.ThemeMode.SYSTEM)
        var themeUpdater: (ThemePrefs.ThemeMode) -> Unit = {}
    }
}
