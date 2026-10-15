package com.michaelamimo.sumeria

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import android.graphics.Color as AndroidColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.fragment.app.FragmentActivity
import com.michaelamimo.sumeria.navigation.Screen
import com.michaelamimo.sumeria.auth.LoginScreen
import com.michaelamimo.sumeria.auth.OnboardingScreen
import com.michaelamimo.sumeria.auth.SignUpScreen
import com.michaelamimo.sumeria.auth.SplashScreen
import com.michaelamimo.sumeria.ui.theme.SumeriaColors
import com.michaelamimo.sumeria.ui.theme.SumeriaTheme
import com.michaelamimo.sumeria.navigation.MainNavigationScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.michaelamimo.sumeria.ui.settings.CloudSyncViewModel

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // REVERTED to default light status bars (which produces dark icons)
        // because the Home screen header is now white again.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT
            )
        )

        setContent {
            SumeriaApp()
        }
    }
}

@Composable
fun SumeriaApp() {
    val settingsViewModel: CloudSyncViewModel = hiltViewModel()
    val settings by settingsViewModel.state.collectAsStateWithLifecycle()
    val darkTheme = when (settings.preferences.appThemeMode) {
        "Light" -> false
        "Dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val navController = rememberNavController()

    SumeriaTheme(darkTheme = darkTheme) {
    Surface(modifier = Modifier.fillMaxSize(), color = if (darkTheme) Color(0xFF121212) else SumeriaColors.SurfaceWhite) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(onSplashFinished = { isLoggedIn ->
                if (isLoggedIn) {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                } else {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            })
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.navigate(Screen.Login.route) }
            )
        }

        // The new Main Navigation Screen that hosts the bottom bar and the 5 tabs
        composable(Screen.Main.route) {
            MainNavigationScreen(onSignedOut = {
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(Screen.Main.route) { inclusive = true }
                }
            })
        }
    }
    }
    }
}
