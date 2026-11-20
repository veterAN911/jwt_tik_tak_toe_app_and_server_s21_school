package com.example.tiktak_app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiktak_app.domain.repository.GameRepository
import com.example.tiktak_app.presentation.isSessionError
import com.example.tiktak_app.presentation.mapper.toViewData
import com.example.tiktak_app.presentation.model.HistoryItemViewData
import com.example.tiktak_app.presentation.toUserMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val items: List<HistoryItemViewData> = emptyList(),
    val isLoading: Boolean = false
)

sealed interface HistoryEvent {
    data class ShowError(val message: String) : HistoryEvent
    data class SessionExpired(val message: String) : HistoryEvent
}

class HistoryViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    private val _events = Channel<HistoryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun load() {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = gameRepository.getFinishedGames()
            _state.update { it.copy(isLoading = false) }

            result
                .onSuccess { games ->
                    _state.update {
                        it.copy(items = games.map { game -> game.toViewData() })
                    }
                }
                .onFailure { report(it) }
        }
    }

    private suspend fun report(error: Throwable) {
        if (error.isSessionError()) {
            _events.send(
                HistoryEvent.SessionExpired(error.toUserMessage("Не удалось загрузить историю игр"))
            )
        } else {
            _events.send(
                HistoryEvent.ShowError(
                    error.toUserMessage("Не удалось загрузить историю игр")
                )
            )
        }
    }
}
