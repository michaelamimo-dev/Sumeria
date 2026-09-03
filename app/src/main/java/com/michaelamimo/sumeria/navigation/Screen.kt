package com.michaelamimo.sumeria.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object SignUp : Screen("sign_up")
    object Home : Screen("home")
}