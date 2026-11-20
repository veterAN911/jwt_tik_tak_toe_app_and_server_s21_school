package com.example.tiktak_app.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tiktak_app.presentation.appViewModel
import com.example.tiktak_app.presentation.model.GameViewData
import com.example.tiktak_app.presentation.viewmodel.GamesEvent
import com.example.tiktak_app.presentation.viewmodel.GamesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(
    onOpenGame: (String) -> Unit,
    onHistory: () -> Unit,
    onLeaderboard: () -> Unit,
    onSessionExpired: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GamesViewModel = appViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadGames()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GamesEvent.ShowError -> onError(event.message)
                is GamesEvent.SessionExpired -> {
                    onError(event.message)
                    onSessionExpired()
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onHistory,
                modifier = Modifier.weight(1f)
            ) {
                Text("История игр")
            }

            Button(
                onClick = onLeaderboard,
                modifier = Modifier.weight(1f)
            ) {
                Text("Таблица лидеров")
            }
        }

        Spacer(Modifier.height(12.dp))

        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.loadGames() },
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            if (state.items.isEmpty()) {
                EmptyMessage("Ожидающих игр нет")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(state.items, key = { it.id }) { game ->
                        GameCard(game = game, onClick = { onOpenGame(game.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun GameCard(game: GameViewData, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "Игра: ${game.id}",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Нажмите, чтобы присоединиться",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
