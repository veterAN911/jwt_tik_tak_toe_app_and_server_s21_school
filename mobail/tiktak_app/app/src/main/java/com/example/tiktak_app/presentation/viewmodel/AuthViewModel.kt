package com.example.tiktak_app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiktak_app.domain.repository.AuthRepository
import com.example.tiktak_app.domain.repository.NoSessionException
import com.example.tiktak_app.presentation.toUserMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AutoLoginState {
    object Checking : AutoLoginState
    object Success : AutoLoginState
    data class NeedLogin(val message: String?) : AutoLoginState
}

sealed interface AuthEvent {
    object LoginSuccess : AuthEvent
    object RegisterSuccess : AuthEvent
    data class ShowError(val message: String) : AuthEvent
}

class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _autoLoginState =
        MutableStateFlow<AutoLoginState>(AutoLoginState.Checking)
    val autoLoginState: StateFlow<AutoLoginState> = _autoLoginState.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var autoLoginStarted = false

    fun startAutoLogin() {
        if (autoLoginStarted) return
        autoLoginStarted = true

        viewModelScope.launch {
            authRepository.autoLogin()
                .onSuccess {
                    _autoLoginState.value = AutoLoginState.Success
                }
                .onFailure { error ->
                    val message = if (error is NoSessionException) {
                        null
                    } else {
                        error.toUserMessage("Не удалось выполнить автоматический вход")
                    }
                    _autoLoginState.value = AutoLoginState.NeedLogin(message)
                }
        }
    }

    fun login(login: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.login(login, password)
            _isLoading.value = false

            result
                .onSuccess { _events.send(AuthEvent.LoginSuccess) }
                .onFailure {
                    _events.send(
                        AuthEvent.ShowError(it.toUserMessage("Ошибка авторизации"))
                    )
                }
        }
    }

    fun register(login: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.register(login, password)
            _isLoading.value = false

            result
                .onSuccess { _events.send(AuthEvent.RegisterSuccess) }
                .onFailure {
                    _events.send(
                        AuthEvent.ShowError(it.toUserMessage("Ошибка регистрации"))
                    )
                }
        }
    }
}
