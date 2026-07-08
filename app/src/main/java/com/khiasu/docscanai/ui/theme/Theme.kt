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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween

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
    val baseColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val animatedColorScheme = ColorScheme(
        primary = animateColorAsState(baseColorScheme.primary, tween(400)).value,
        onPrimary = animateColorAsState(baseColorScheme.onPrimary, tween(400)).value,
        primaryContainer = animateColorAsState(baseColorScheme.primaryContainer, tween(400)).value,
        onPrimaryContainer = animateColorAsState(baseColorScheme.onPrimaryContainer, tween(400)).value,
        secondary = animateColorAsState(baseColorScheme.secondary, tween(400)).value,
        onSecondary = animateColorAsState(baseColorScheme.onSecondary, tween(400)).value,
        secondaryContainer = animateColorAsState(baseColorScheme.secondaryContainer, tween(400)).value,
        onSecondaryContainer = animateColorAsState(baseColorScheme.onSecondaryContainer, tween(400)).value,
        tertiary = animateColorAsState(baseColorScheme.tertiary, tween(400)).value,
        onTertiary = animateColorAsState(baseColorScheme.onTertiary, tween(400)).value,
        tertiaryContainer = animateColorAsState(baseColorScheme.tertiaryContainer, tween(400)).value,
        onTertiaryContainer = animateColorAsState(baseColorScheme.onTertiaryContainer, tween(400)).value,
        background = animateColorAsState(baseColorScheme.background, tween(400)).value,
        onBackground = animateColorAsState(baseColorScheme.onBackground, tween(400)).value,
        surface = animateColorAsState(baseColorScheme.surface, tween(400)).value,
        onSurface = animateColorAsState(baseColorScheme.onSurface, tween(400)).value,
        surfaceVariant = animateColorAsState(baseColorScheme.surfaceVariant, tween(400)).value,
        onSurfaceVariant = animateColorAsState(baseColorScheme.onSurfaceVariant, tween(400)).value,
        surfaceTint = animateColorAsState(baseColorScheme.surfaceTint, tween(400)).value,
        outline = animateColorAsState(baseColorScheme.outline, tween(400)).value,
        outlineVariant = animateColorAsState(baseColorScheme.outlineVariant, tween(400)).value,
        error = animateColorAsState(baseColorScheme.error, tween(400)).value,
        onError = animateColorAsState(baseColorScheme.onError, tween(400)).value,
        errorContainer = animateColorAsState(baseColorScheme.errorContainer, tween(400)).value,
        onErrorContainer = animateColorAsState(baseColorScheme.onErrorContainer, tween(400)).value,
        inverseOnSurface = animateColorAsState(baseColorScheme.inverseOnSurface, tween(400)).value,
        inverseSurface = animateColorAsState(baseColorScheme.inverseSurface, tween(400)).value,
        inversePrimary = animateColorAsState(baseColorScheme.inversePrimary, tween(400)).value,
        scrim = animateColorAsState(baseColorScheme.scrim, tween(400)).value,
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = baseColorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = ScanWiseTypography,
        content = content
    )
}
