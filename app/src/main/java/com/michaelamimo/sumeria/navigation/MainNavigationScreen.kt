package com.michaelamimo.sumeria.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.michaelamimo.sumeria.ui.home.HomeScreen
import com.michaelamimo.sumeria.ui.theme.SumeriaColors

@Composable
fun MainNavigationScreen() {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute =
        navBackStackEntry?.destination?.route ?: Screen.Home.route

    Scaffold(
        bottomBar = {
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
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(
                bottom = innerPadding.calculateBottomPadding()
            )
        ) {
            composable(Screen.Home.route) {
                HomeScreen()
            }

            composable(Screen.Library.route) {
                PlaceholderScreen("Library")
            }

            composable(Screen.Reader.route) {
                PlaceholderScreen("Reader")
            }

            composable(Screen.Stats.route) {
                PlaceholderScreen("Stats")
            }

            composable(Screen.Settings.route) {
                PlaceholderScreen("Settings")
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

    // Continuous Sumeria-green navigation background
    Surface(
        color = SumeriaColors.ActionPrimary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(62.dp)
                .padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route

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
                        .clickable {
                            onNavigate(item.route)
                        }
                        .padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        ),
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
                        modifier = Modifier.size(26.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        text = item.label,
                        color = if (isSelected) {
                            SumeriaColors.SurfaceWhite
                        } else {
                            SumeriaColors.Muted.copy(alpha = 0.70f)
                        },
                        fontSize = 10.sp,
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
        // Dark green area behind the status bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SumeriaColors.ActionPrimary)
                .windowInsetsTopHeight(WindowInsets.statusBars)
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = SumeriaColors.TextPrimary
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "More coming soon",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SumeriaColors.Muted
                )
            }
        }
    }
}