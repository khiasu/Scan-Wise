package com.khiasu.docscanai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.khiasu.docscanai.prefs.ThemePrefs
import com.khiasu.docscanai.ui.AppRoot
import com.khiasu.docscanai.ui.theme.ScanWiseTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }

                    if (showSplash) {
                        SplashScreen(onFinished = { showSplash = false })
                    } else {
                        AppRoot()
                    }
                }
            }
        }
    }

    companion object {
        var currentThemeMode by mutableStateOf(ThemePrefs.ThemeMode.SYSTEM)
        var themeUpdater: (ThemePrefs.ThemeMode) -> Unit = {}
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.9f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        delay(1000)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "ScanWise Logo",
            modifier = Modifier
                .size(160.dp)
                .scale(scale.value)
        )
    }
}
