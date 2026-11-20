package com.example.tiktak_app.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tiktak_app.presentation.ui.CurrentGameScreen
import com.example.tiktak_app.presentation.ui.GamesScreen
import com.example.tiktak_app.presentation.ui.HistoryScreen
import com.example.tiktak_app.presentation.ui.LeaderboardScreen
import com.example.tiktak_app.presentation.ui.LoginScreen
import com.example.tiktak_app.presentation.ui.MenuScreen
import com.example.tiktak_app.presentation.ui.RegisterScreen
import com.example.tiktak_app.presentation.ui.SplashScreen
import kotlinx.coroutines.launch

private object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MENU = "menu"
    const val GAMES = "games"
    const val HISTORY = "history"
    const val LEADERBOARD = "leaderboard"
    const val GAME = "game/{gameId}/{join}"

    fun game(gameId: String, join: Boolean) = "game/$gameId/$join"
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showError: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onSuccess = {
                        navController.navigate(Routes.MENU) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    },
                    onNeedLogin = { message ->
                        message?.let(showError)
                        navController.goToLogin()
                    }
                )
            }

            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.MENU) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onRegister = { navController.navigate(Routes.REGISTER) },
                    onError = showError
                )
            }

            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegistered = { navController.popBackStack() },
                    onError = showError
                )
            }

            composable(Routes.MENU) {
                MenuScreen(
                    onGames = { navController.navigate(Routes.GAMES) },
                    onGameCreated = { gameId ->
                        navController.navigate(Routes.game(gameId, false))
                    },
                    onLoggedOut = { navController.goToLogin() },
                    onSessionExpired = { navController.goToLogin() },
                    onError = showError
                )
            }

            composable(Routes.GAMES) {
                GamesScreen(
                    onOpenGame = { gameId ->
                        navController.navigate(Routes.game(gameId, true))
                    },
                    onHistory = { navController.navigate(Routes.HISTORY) },
                    onLeaderboard = { navController.navigate(Routes.LEADERBOARD) },
                    onSessionExpired = { navController.goToLogin() },
                    onError = showError
                )
            }

            composable(Routes.HISTORY) {
                HistoryScreen(
                    onSessionExpired = { navController.goToLogin() },
                    onError = showError
                )
            }

            composable(Routes.LEADERBOARD) {
                LeaderboardScreen(
                    onSessionExpired = { navController.goToLogin() },
                    onError = showError
                )
            }

            composable(
                route = Routes.GAME,
                arguments = listOf(
                    navArgument("gameId") { type = NavType.StringType },
                    navArgument("join") { type = NavType.BoolType }
                )
            ) { entry ->
                CurrentGameScreen(
                    gameId = entry.arguments?.getString("gameId").orEmpty(),
                    join = entry.arguments?.getBoolean("join") ?: false,
                    onSessionExpired = { navController.goToLogin() },
                    onError = showError
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

private fun NavController.goToLogin() {
    navigate(Routes.LOGIN) {
        popUpTo(graph.id) { inclusive = true }
    }
}
