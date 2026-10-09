package com.michaelamimo.sumeria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.michaelamimo.sumeria.navigation.Screen
import com.michaelamimo.sumeria.auth.LoginScreen
import com.michaelamimo.sumeria.auth.OnboardingScreen
import com.michaelamimo.sumeria.auth.SignUpScreen
import com.michaelamimo.sumeria.auth.SplashScreen
import com.michaelamimo.sumeria.ui.theme.SumeriaColors
import com.michaelamimo.sumeria.ui.theme.SumeriaTheme
import com.michaelamimo.sumeria.navigation.MainNavigationScreen

class MainActivity : ComponentActivity() {
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
            SumeriaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SumeriaColors.SurfaceWhite
                ) {
                    SumeriaApp()
                }
            }
        }
    }
}

@Composable
fun SumeriaApp() {
    val navController = rememberNavController()

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
            MainNavigationScreen()
        }
    }
}