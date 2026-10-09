package com.michaelamimo.sumeria.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object SignUp : Screen("sign_up")

    // The main container that holds the bottom navigation bar
    object Main : Screen("main")

    // The 5 individual tabs
    object Home : Screen("home")
    object Library : Screen("library")
    object Reader : Screen("reader")
    object Stats : Screen("stats")
    object Settings : Screen("settings")
}