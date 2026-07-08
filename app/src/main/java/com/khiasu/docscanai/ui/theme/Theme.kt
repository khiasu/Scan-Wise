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
    background = Dark100,
    onBackground = OnDark,
    surface = Dark90,
    onSurface = OnDark,
    surfaceVariant = Dark70,
    onSurfaceVariant = OnDarkMuted,
    surfaceContainerLowest = Dark100,
    surfaceContainerLow = Dark90,
    surfaceContainer = Dark80,
    surfaceContainerHigh = Dark70,
    surfaceContainerHighest = Dark60,
    outline = Color(0xFF3D3D50),
    outlineVariant = Color(0xFF2A2A3C),
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
    background = Cream100,
    onBackground = OnCream,
    surface = Color.White,
    onSurface = OnCream,
    surfaceVariant = Cream90,
    onSurfaceVariant = OnCreamMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Cream100,
    surfaceContainer = Cream90,
    surfaceContainerHigh = Cream80,
    surfaceContainerHighest = Cream70,
    outline = Color(0xFFD0D0D8),
    outlineVariant = Color(0xFFE8E0D8),
    error = Color(0xFFDC2626),
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
