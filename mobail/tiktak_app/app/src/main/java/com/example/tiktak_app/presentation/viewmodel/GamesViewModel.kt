package com.example.tiktak_app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiktak_app.domain.model.Game
import com.example.tiktak_app.domain.repository.AuthRepository
import com.example.tiktak_app.domain.repository.GameRepository
import com.example.tiktak_app.presentation.isSessionError
import com.example.tiktak_app.presentation.mapper.toViewData
import com.example.tiktak_app.presentation.model.GameViewData
import com.example.tiktak_app.presentation.toUserMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GamesUiState(
    val items: List<GameViewData> = emptyList(),
    val isLoading: Boolean = false
)

sealed interface GamesEvent {
    data class ShowError(val message: String) : GamesEvent
    data class SessionExpired(val message: String) : GamesEvent
}

class GamesViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GamesUiState())
    val state: StateFlow<GamesUiState> = _state.asStateFlow()

    private val _events = Channel<GamesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun loadGames() {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val userId = authRepository.getCurrentUser()?.id
            val result = gameRepository.getWaitingGames()
            _state.update { it.copy(isLoading = false) }

            result
                .onSuccess { games ->
                    val items = games
                        .filter { it.status == Game.STATUS_WAITING }
                        .filter { it.playerX != userId }
                        .map { it.toViewData() }
                    _state.update { it.copy(items = items) }
                }
                .onFailure { report(it, "Не удалось загрузить список игр") }
        }
    }

    private suspend fun report(error: Throwable, fallback: String) {
        if (error.isSessionError()) {
            _events.send(GamesEvent.SessionExpired(error.toUserMessage(fallback)))
        } else {
            _events.send(GamesEvent.ShowError(error.toUserMessage(fallback)))
        }
    }
}
