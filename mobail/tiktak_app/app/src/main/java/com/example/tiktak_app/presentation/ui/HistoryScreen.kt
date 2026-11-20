package com.example.tiktak_app.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tiktak_app.R
import com.example.tiktak_app.domain.model.FinishedGame
import com.example.tiktak_app.presentation.appViewModel
import com.example.tiktak_app.presentation.model.HistoryItemViewData
import com.example.tiktak_app.presentation.viewmodel.HistoryEvent
import com.example.tiktak_app.presentation.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onSessionExpired: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = appViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HistoryEvent.ShowError -> onError(event.message)
                is HistoryEvent.SessionExpired -> {
                    onError(event.message)
                    onSessionExpired()
                }
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { viewModel.load() },
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) {
        if (state.items.isEmpty()) {
            EmptyMessage("Завершённых игр пока нет")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(state.items, key = { it.gameId }) { item ->
                    HistoryCard(item)
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(item: HistoryItemViewData) {
    val resultColor = when (item.result) {
        FinishedGame.RESULT_WIN -> colorResource(R.color.result_win)
        FinishedGame.RESULT_DRAW -> colorResource(R.color.result_draw)
        else -> colorResource(R.color.result_loss)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "Игра: ${item.gameId}",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Соперник: ${item.opponentLogin}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.resultText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = resultColor
            )
        }
    }
}
