package com.example.tiktak_app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiktak_app.domain.model.Game
import com.example.tiktak_app.domain.repository.AuthRepository
import com.example.tiktak_app.domain.repository.GameEvent
import com.example.tiktak_app.domain.repository.GameRepository
import com.example.tiktak_app.domain.repository.GameSession
import com.example.tiktak_app.presentation.isSessionError
import com.example.tiktak_app.presentation.mapper.toViewData
import com.example.tiktak_app.presentation.model.GameViewData
import com.example.tiktak_app.presentation.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CurrentGameUiState(
    val game: GameViewData? = null,
    val currentUserId: String? = null,
    val isLoading: Boolean = false
)

sealed interface CurrentGameEvent {
    data class ShowError(val message: String) : CurrentGameEvent
    data class SessionExpired(val message: String) : CurrentGameEvent
}

class CurrentGameViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CurrentGameUiState())
    val state: StateFlow<CurrentGameUiState> = _state.asStateFlow()

    private val _events = Channel<CurrentGameEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var sessionJob: Job? = null
    private var gameSession: GameSession? = null

    fun start(gameId: String, join: Boolean) {
        if (gameId.isBlank() || sessionJob?.isActive == true) return

        sessionJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val user = authRepository.getCurrentUser()
            if (user == null) {
                _events.send(CurrentGameEvent.SessionExpired("Сессия истекла, войдите заново"))
                return@launch
            }

            _state.update { it.copy(currentUserId = user.id) }

            val openResult = gameRepository.openGame(gameId, join)
            val session = openResult.getOrElse { error ->
                _state.update { it.copy(isLoading = false) }
                report(error, "Не удалось подключиться к игре")
                return@launch
            }

            gameSession = session

            try {
                session.events.collect { event ->
                    when (event) {
                        is GameEvent.GameUpdated -> {
                            _state.update {
                                it.copy(
                                    game = event.game.toViewData(),
                                    isLoading = false
                                )
                            }
                        }

                        is GameEvent.Error -> {
                            _state.update { it.copy(isLoading = false) }
                            if (event.message.contains("токен", ignoreCase = true)) {
                                _events.send(CurrentGameEvent.SessionExpired(event.message))
                            } else {
                                _events.send(CurrentGameEvent.ShowError(event.message))
                            }
                        }
                    }
                }
            } finally {
                session.close()
                gameSession = null
            }
        }
    }

    fun stop() {
        gameSession?.close()
        gameSession = null
        sessionJob?.cancel()
        sessionJob = null
    }

    fun makeMove(row: Int, col: Int) {
        val state = _state.value
        val game = state.game ?: return
        val userId = state.currentUserId ?: return

        val symbol = when (userId) {
            game.playerX -> Game.SYMBOL_X
            game.playerO -> Game.SYMBOL_O
            else -> Game.EMPTY_CELL
        }
        if (symbol == Game.EMPTY_CELL) return

        val board = game.board.mapIndexed { rowIndex, cells ->
            cells.mapIndexed { colIndex, cell ->
                if (rowIndex == row && colIndex == col) symbol else cell
            }
        }

        gameSession?.makeMove(board)
    }

    private suspend fun report(error: Throwable, fallback: String) {
        if (error.isSessionError()) {
            _events.send(CurrentGameEvent.SessionExpired(error.toUserMessage(fallback)))
        } else {
            _events.send(CurrentGameEvent.ShowError(error.toUserMessage(fallback)))
        }
    }
}
