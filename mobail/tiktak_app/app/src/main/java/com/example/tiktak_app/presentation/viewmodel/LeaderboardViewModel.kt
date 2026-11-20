package com.example.tiktak_app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiktak_app.domain.repository.GameRepository
import com.example.tiktak_app.presentation.isSessionError
import com.example.tiktak_app.presentation.mapper.toViewData
import com.example.tiktak_app.presentation.model.LeaderboardItemViewData
import com.example.tiktak_app.presentation.toUserMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaderboardUiState(
    val items: List<LeaderboardItemViewData> = emptyList(),
    val isLoading: Boolean = false
)

sealed interface LeaderboardEvent {
    data class ShowError(val message: String) : LeaderboardEvent
    data class SessionExpired(val message: String) : LeaderboardEvent
}

class LeaderboardViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LeaderboardUiState())
    val state: StateFlow<LeaderboardUiState> = _state.asStateFlow()

    private val _events = Channel<LeaderboardEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun load() {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = gameRepository.getLeaderboard()
            _state.update { it.copy(isLoading = false) }

            result
                .onSuccess { players ->
                    _state.update {
                        it.copy(
                            items = players.mapIndexed { index, player ->
                                player.toViewData(position = index + 1)
                            }
                        )
                    }
                }
                .onFailure { report(it) }
        }
    }

    private suspend fun report(error: Throwable) {
        if (error.isSessionError()) {
            _events.send(
                LeaderboardEvent.SessionExpired(
                    error.toUserMessage("Не удалось загрузить таблицу лидеров")
                )
            )
        } else {
            _events.send(
                LeaderboardEvent.ShowError(
                    error.toUserMessage("Не удалось загрузить таблицу лидеров")
                )
            )
        }
    }
}
