package com.example.tiktak_app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiktak_app.domain.model.GameMode
import com.example.tiktak_app.domain.repository.AuthRepository
import com.example.tiktak_app.domain.repository.GameRepository
import com.example.tiktak_app.presentation.isSessionError
import com.example.tiktak_app.presentation.mapper.toViewData
import com.example.tiktak_app.presentation.model.UserViewData
import com.example.tiktak_app.presentation.toUserMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MenuUiState(
    val user: UserViewData? = null,
    val isLoading: Boolean = false
)

sealed interface MenuEvent {
    data class GameCreated(val gameId: String) : MenuEvent
    data class ShowError(val message: String) : MenuEvent
    data class SessionExpired(val message: String) : MenuEvent
    object LoggedOut : MenuEvent
}

class MenuViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MenuUiState())
    val state: StateFlow<MenuUiState> = _state.asStateFlow()

    private val _events = Channel<MenuEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadUser()
    }

    fun loadUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()?.toViewData()
            _state.update { it.copy(user = user) }
        }
    }

    fun createGame(mode: GameMode) {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = gameRepository.createGame(mode)
            _state.update { it.copy(isLoading = false) }

            result
                .onSuccess { _events.send(MenuEvent.GameCreated(it.id)) }
                .onFailure { report(it, "Не удалось создать игру") }
        }
    }

    fun logout() {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching { authRepository.logout() }
            _state.update { it.copy(isLoading = false) }
            _events.send(MenuEvent.LoggedOut)
        }
    }

    private suspend fun report(error: Throwable, fallback: String) {
        if (error.isSessionError()) {
            _events.send(MenuEvent.SessionExpired(error.toUserMessage(fallback)))
        } else {
            _events.send(MenuEvent.ShowError(error.toUserMessage(fallback)))
        }
    }
}
