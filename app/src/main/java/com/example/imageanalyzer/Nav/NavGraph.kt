package com.example.imageanalyzer.Nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.imageanalyzer.BakingScreen
import com.example.imageanalyzer.Presentation.Screens.HistoryScreen
import com.example.imageanalyzer.Presentation.Screens.LoginScreen
import com.example.imageanalyzer.Presentation.Screens.ProfileScreen
import com.example.imageanalyzer.Presentation.Screens.SignupScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

@Composable
fun NavGraph(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()

    val user by produceState<FirebaseUser?>(
        initialValue = auth.currentUser,
        key1 = auth
    ) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            value = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)

        awaitDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    val startDestination = if (user != null) Screen.Baking.route else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth Screens
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Baking.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                }
            )
        }

        composable(Screen.SignUp.route) {
            SignupScreen(
                onSignUpSuccess = {
                    navController.navigate(Screen.Baking.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Main App Screens
        composable(Screen.Baking.route) {
            BakingScreen(
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0)
                    }
                }
            )
        }
    }
}