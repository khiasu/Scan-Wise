package com.khiasu.docscanai.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

private sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Scan : Dest("scan", "Scan", Icons.Default.CameraAlt)
    data object Documents : Dest("documents", "Documents", Icons.Default.Description)
    data object Settings : Dest("settings", "Settings", Icons.Default.Settings)
}

private const val DETAIL_ROUTE = "document/{docId}"

@Composable
fun AppRoot() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { fadeIn(animationSpec = tween(220)) },
        exitTransition = { fadeOut(animationSpec = tween(220)) }
    ) {
        composable("home") {
            HomePagerContainer(
                onNavigateToDetail = { docId ->
                    navController.navigate("document/$docId")
                }
            )
        }
        composable(DETAIL_ROUTE) { backStackEntry ->
            val docId = backStackEntry.arguments?.getString("docId")?.toLongOrNull() ?: return@composable
            DocumentDetailScreen(docId)
        }
    }
}

@Composable
fun HomePagerContainer(onNavigateToDetail: (Long) -> Unit) {
    val tabs = listOf(Dest.Scan, Dest.Documents, Dest.Settings)
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, dest ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding)
        ) { page ->
            when (page) {
                0 -> ScanScreen(onDocumentCreated = onNavigateToDetail)
                1 -> DocumentsScreen(onOpenDocument = onNavigateToDetail)
                2 -> SettingsScreen()
            }
        }
    }
}
