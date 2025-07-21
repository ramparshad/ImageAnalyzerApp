package com.example.imageanalyzer.Nav

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Baking : Screen("baking")
    object History : Screen("history")
    object Profile : Screen("profile")

    companion object {
        // Optional
        fun getStartDestination(isLoggedIn: Boolean): String {
            return if (isLoggedIn) Baking.route else Login.route
        }
    }
}
