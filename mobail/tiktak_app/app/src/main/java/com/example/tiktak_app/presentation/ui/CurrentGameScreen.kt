package com.example.tiktak_app.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tiktak_app.presentation.appViewModel
import com.example.tiktak_app.presentation.model.GameViewData
import com.example.tiktak_app.presentation.viewmodel.CurrentGameEvent
import com.example.tiktak_app.presentation.viewmodel.CurrentGameViewModel

@Composable
fun CurrentGameScreen(
    gameId: String,
    join: Boolean,
    onSessionExpired: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CurrentGameViewModel = appViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(gameId, join) {
        viewModel.start(gameId, join)
    }

    DisposableEffect(viewModel) {
        onDispose { viewModel.stop() }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CurrentGameEvent.ShowError -> onError(event.message)
                is CurrentGameEvent.SessionExpired -> {
                    onError(event.message)
                    onSessionExpired()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Игра: ${state.game?.id ?: gameId}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = state.game?.statusText(state.currentUserId) ?: "Подключение...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (state.isLoading && state.game == null) {
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator()
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "X: ${state.game?.let { it.playerLabel(it.playerX, state.currentUserId) } ?: "..."}",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "O: ${state.game?.let { it.playerLabel(it.playerO, state.currentUserId) } ?: "..."}",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(24.dp))

        Board(
            game = state.game,
            onCellClick = viewModel::makeMove
        )
    }
}

@Composable
private fun Board(
    game: GameViewData?,
    onCellClick: (Int, Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(0.85f)) {
        repeat(3) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(3) { col ->
                    Button(
                        onClick = { onCellClick(row, col) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = game?.symbolAt(row, col).orEmpty(),
                            fontSize = 32.sp
                        )
                    }
                }
            }
        }
    }
}
