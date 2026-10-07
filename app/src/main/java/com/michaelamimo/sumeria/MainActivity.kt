package com.michaelamimo.sumeria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MODIFIED: 'SystemBarStyle.dark' forces the status bar icons to be white
        // so they can be seen clearly on our new Dark Green status bar background.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                AndroidColor.TRANSPARENT
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
                    navController.navigate(Screen.Home.route) {
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
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.navigate(Screen.Login.route) }
            )
        }

        composable(Screen.Home.route) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Library Home Screen (Phase 2)", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}