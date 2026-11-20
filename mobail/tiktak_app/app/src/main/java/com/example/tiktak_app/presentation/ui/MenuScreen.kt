package com.example.tiktak_app.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tiktak_app.domain.model.GameMode
import com.example.tiktak_app.presentation.appViewModel
import com.example.tiktak_app.presentation.viewmodel.MenuEvent
import com.example.tiktak_app.presentation.viewmodel.MenuViewModel

@Composable
fun MenuScreen(
    onGames: () -> Unit,
    onGameCreated: (String) -> Unit,
    onLoggedOut: () -> Unit,
    onSessionExpired: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MenuViewModel = appViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MenuEvent.GameCreated -> onGameCreated(event.gameId)
                is MenuEvent.ShowError -> onError(event.message)
                is MenuEvent.SessionExpired -> {
                    onError(event.message)
                    onSessionExpired()
                }
                MenuEvent.LoggedOut -> onLoggedOut()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Вы: ${state.user?.login ?: "..."}",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = { viewModel.createGame(GameMode.BOT) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Игра с ботом")
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { viewModel.createGame(GameMode.HUMAN) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Игра с человеком")
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onGames,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Список игр")
        }

        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick = { viewModel.logout() },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Выйти")
        }

        if (state.isLoading) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}
