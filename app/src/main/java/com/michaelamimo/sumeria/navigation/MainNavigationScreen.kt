package com.michaelamimo.sumeria.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.michaelamimo.sumeria.ui.home.HomeScreen
import com.michaelamimo.sumeria.ui.library.LibraryScreen
import com.michaelamimo.sumeria.ui.reader.ReaderScreen
import com.michaelamimo.sumeria.ui.settings.SettingsScreen
import com.michaelamimo.sumeria.ui.stats.StatsScreen
import com.michaelamimo.sumeria.ui.theme.SumeriaColors

@Composable
fun MainNavigationScreen(onSignedOut: () -> Unit) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route
    val isReaderScreen = currentRoute == Screen.Reader.route || currentRoute == Screen.Reader.bookRoute

    Scaffold(
        bottomBar = {
            // Hide the bottom navigation bar completely when in the Reader.
            if (!isReaderScreen) {
                SumeriaBottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        bottomNavController.navigate(route) {
                            popUpTo(
                                bottomNavController.graph.findStartDestination().id
                            ) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(
                bottom = if (isReaderScreen) {
                    0.dp
                } else {
                    innerPadding.calculateBottomPadding()
                }
            )
        ) {
            composable(Screen.Home.route) {
                HomeScreen()
            }

            composable(Screen.Library.route) {
                LibraryScreen(onBookClick = { bookId ->
                    bottomNavController.navigate(Screen.Reader.forBook(bookId))
                })
            }

            composable(Screen.Reader.route) {
                ReaderScreen(
                    onNavigateBack = {
                        if (!bottomNavController.popBackStack()) {
                            bottomNavController.navigate(Screen.Home.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            composable(
                route = Screen.Reader.bookRoute,
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
            ) {
                ReaderScreen(
                    onNavigateBack = {
                        if (!bottomNavController.popBackStack()) {
                            bottomNavController.navigate(Screen.Library.route) { launchSingleTop = true }
                        }
                    }
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(onSignOut = onSignedOut)
            }
        }
    }
}

@Composable
fun SumeriaBottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        NavigationItem(
            route = Screen.Home.route,
            label = "Home",
            icon = Icons.Filled.Home
        ),
        NavigationItem(
            route = Screen.Library.route,
            label = "Library",
            icon = Icons.Outlined.LibraryBooks
        ),
        NavigationItem(
            route = Screen.Reader.route,
            label = "Reader",
            icon = Icons.Outlined.MenuBook
        ),
        NavigationItem(
            route = Screen.Stats.route,
            label = "Stats",
            icon = Icons.Outlined.Insights
        ),
        NavigationItem(
            route = Screen.Settings.route,
            label = "Settings",
            icon = Icons.Outlined.Settings
        )
    )

    Surface(
        color = SumeriaColors.ActionPrimary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(52.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route

                // Equal-width cells keep the icons spread across the bar.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onNavigate(item.route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) {
                                    SumeriaColors.SurfaceWhite.copy(alpha = 0.14f)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) {
                                SumeriaColors.SurfaceWhite
                            } else {
                                SumeriaColors.Muted.copy(alpha = 0.70f)
                            },
                            modifier = Modifier.size(23.dp)
                        )

                        Spacer(Modifier.height(1.dp))

                        Text(
                            text = item.label,
                            color = if (isSelected) {
                                SumeriaColors.SurfaceWhite
                            } else {
                                SumeriaColors.Muted.copy(alpha = 0.70f)
                            },
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            },
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private data class NavigationItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun PlaceholderScreen(title: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SumeriaColors.SurfaceWhite)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = SumeriaColors.TextPrimary
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "More coming soon",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SumeriaColors.Muted
                )
            }
        }
    }
}
