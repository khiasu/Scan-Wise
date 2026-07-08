package com.khiasu.docscanai.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Scan : Dest("scan", "Scan", Icons.Default.CameraAlt)
    data object Documents : Dest("documents", "Documents", Icons.Default.Description)
    data object Settings : Dest("settings", "Settings", Icons.Default.Settings)
}

private const val DETAIL_ROUTE = "document/{docId}"

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val tabs = listOf(Dest.Scan, Dest.Documents, Dest.Settings)

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar {
                tabs.forEach { dest ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Scan.route,
            modifier = Modifier.padding(padding),
            enterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                val direction = if (getTabOrder(initialRoute) < getTabOrder(targetRoute)) {
                    AnimatedContentTransitionScope.SlideDirection.Left
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Right
                }
                slideIntoContainer(direction, animationSpec = tween(350)) + fadeIn(animationSpec = tween(250))
            },
            exitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                val direction = if (getTabOrder(initialRoute) < getTabOrder(targetRoute)) {
                    AnimatedContentTransitionScope.SlideDirection.Left
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Right
                }
                slideOutOfContainer(direction, animationSpec = tween(350)) + fadeOut(animationSpec = tween(250))
            }
        ) {
            composable(Dest.Scan.route) {
                ScanScreen(onDocumentCreated = { docId ->
                    navController.navigate("document/$docId")
                })
            }
            composable(Dest.Documents.route) {
                DocumentsScreen(onOpenDocument = { docId -> navController.navigate("document/$docId") })
            }
            composable(Dest.Settings.route) { SettingsScreen() }
            composable(DETAIL_ROUTE) { backStackEntry ->
                val docId = backStackEntry.arguments?.getString("docId")?.toLongOrNull() ?: return@composable
                DocumentDetailScreen(docId)
            }
        }
    }
}

private fun getTabOrder(route: String?): Int {
    return when (route) {
        "scan" -> 0
        "documents" -> 1
        "settings" -> 2
        else -> 3
    }
}
