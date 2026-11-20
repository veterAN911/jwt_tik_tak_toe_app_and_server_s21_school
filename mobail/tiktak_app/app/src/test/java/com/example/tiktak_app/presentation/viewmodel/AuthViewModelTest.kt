package com.example.tiktak_app.presentation.viewmodel

import com.example.tiktak_app.FakeAuthRepository
import com.example.tiktak_app.MainDispatcherRule
import com.example.tiktak_app.domain.repository.ApiException
import com.example.tiktak_app.domain.repository.NoSessionException
import com.example.tiktak_app.testUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        authRepository = FakeAuthRepository()
        viewModel = AuthViewModel(authRepository)
    }

    private fun TestScope.collectEvents(): MutableList<AuthEvent> {
        val events = mutableListOf<AuthEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events += it }
        }
        return events
    }

    @Test
    fun `login sends entered data to repository without local checks`() = runTest {
        val events = collectEvents()

        viewModel.login("", "")

        assertEquals(listOf(AuthEvent.LoginSuccess), events)
        assertEquals(1, authRepository.loginCalls)
        assertEquals("" to "", authRepository.lastLogin)
    }

    @Test
    fun `successful login emits login success`() = runTest {
        val events = collectEvents()

        viewModel.login("player", "secret")

        assertEquals(listOf(AuthEvent.LoginSuccess), events)
        assertEquals("player" to "secret", authRepository.lastLogin)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `failed login shows server error`() = runTest {
        authRepository.loginResult = Result.failure(ApiException(400, "Неверный пароль"))
        val events = collectEvents()

        viewModel.login("player", "wrong")

        assertEquals(listOf(AuthEvent.ShowError("Неверный пароль")), events)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `register sends empty data to repository without local checks`() = runTest {
        val events = collectEvents()

        viewModel.register("", "")

        assertEquals(listOf(AuthEvent.RegisterSuccess), events)
        assertEquals(1, authRepository.registerCalls)
    }

    @Test
    fun `successful register emits register success`() = runTest {
        val events = collectEvents()

        viewModel.register("player", "secret")

        assertEquals(listOf(AuthEvent.RegisterSuccess), events)
        assertEquals(1, authRepository.registerCalls)
    }

    @Test
    fun `failed register shows server error`() = runTest {
        authRepository.registerResult = Result.failure(ApiException(409, "Логин занят"))
        val events = collectEvents()

        viewModel.register("player", "secret")

        assertEquals(listOf(AuthEvent.ShowError("Логин занят")), events)
    }

    @Test
    fun `auto login success moves to success state`() {
        viewModel.startAutoLogin()

        assertEquals(AutoLoginState.Success, viewModel.autoLoginState.value)
        assertEquals(1, authRepository.autoLoginCalls)
    }

    @Test
    fun `auto login runs only once`() {
        viewModel.startAutoLogin()
        viewModel.startAutoLogin()
        viewModel.startAutoLogin()

        assertEquals(1, authRepository.autoLoginCalls)
    }

    @Test
    fun `auto login without saved session asks for login without message`() {
        authRepository.autoLoginResult = Result.failure(NoSessionException())

        viewModel.startAutoLogin()

        assertEquals(AutoLoginState.NeedLogin(null), viewModel.autoLoginState.value)
    }

    @Test
    fun `auto login failure shows error message`() {
        authRepository.autoLoginResult =
            Result.failure(ApiException(401, "Сессия истекла, войдите заново"))

        viewModel.startAutoLogin()

        assertEquals(
            AutoLoginState.NeedLogin("Сессия истекла, войдите заново"),
            viewModel.autoLoginState.value
        )
    }

    @Test
    fun `auto login keeps profile on success`() {
        authRepository.autoLoginResult = Result.success(testUser(login = "saved"))

        viewModel.startAutoLogin()

        assertEquals(AutoLoginState.Success, viewModel.autoLoginState.value)
    }
}
