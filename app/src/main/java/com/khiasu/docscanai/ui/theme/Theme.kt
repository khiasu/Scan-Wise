package com.khiasu.docscanai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple50,
    onPrimary = Color.White,
    primaryContainer = Purple05,
    onPrimaryContainer = Purple80,
    secondary = Purple60,
    onSecondary = Color.White,
    secondaryContainer = Purple10,
    onSecondaryContainer = Purple60,
    tertiary = Purple40,
    onTertiary = Color.White,
    background = Color(0xFF000000), // AMOLED Pure Black
    onBackground = OnDark,
    surface = Color(0xFF0B0A0F), // Very deep purple-black
    onSurface = OnDark,
    surfaceVariant = Color(0xFF14131C), // Deep purple-gray
    onSurfaceVariant = OnDarkMuted,
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF07060A),
    surfaceContainer = Color(0xFF0D0C12),
    surfaceContainerHigh = Color(0xFF14131A),
    surfaceContainerHighest = Color(0xFF1B1A24),
    outline = Color(0xFF2E2D3A),
    outlineVariant = Color(0xFF1D1C26),
    error = StatusError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE5FF),
    onPrimaryContainer = Purple20,
    secondary = Purple30,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0E8FF),
    onSecondaryContainer = Purple30,
    tertiary = Purple50,
    onTertiary = Color.White,
    background = Slate50,
    onBackground = OnCream,
    surface = Color.White,
    onSurface = OnCream,
    surfaceVariant = Slate100,
    onSurfaceVariant = OnCreamMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Slate50,
    surfaceContainer = Slate100,
    surfaceContainerHigh = Color(0xFFE5E7EB),
    surfaceContainerHighest = Color(0xFFD1D5DB),
    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFFE5E7EB),
    error = Color(0xFFEF4444),
    onError = Color.White
)

@Composable
fun ScanWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ScanWiseTypography,
        content = content
    )
}
