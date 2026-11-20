package com.example.tiktak_app.presentation.viewmodel

import com.example.tiktak_app.FakeAuthRepository
import com.example.tiktak_app.FakeGameRepository
import com.example.tiktak_app.MainDispatcherRule
import com.example.tiktak_app.domain.model.GameMode
import com.example.tiktak_app.domain.repository.ApiException
import com.example.tiktak_app.testGame
import com.example.tiktak_app.testUser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MenuViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var gameRepository: FakeGameRepository
    private lateinit var viewModel: MenuViewModel

    @Before
    fun setUp() {
        authRepository = FakeAuthRepository()
        gameRepository = FakeGameRepository()
        viewModel = MenuViewModel(authRepository, gameRepository)
    }

    private fun TestScope.collectEvents(): MutableList<MenuEvent> {
        val events = mutableListOf<MenuEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events += it }
        }
        return events
    }

    @Test
    fun `current user is loaded on start`() {
        authRepository.currentUser = testUser(login = "saved")

        viewModel.loadUser()

        assertEquals("saved", viewModel.state.value.user?.login)
    }

    @Test
    fun `creating bot game emits game id and uses bot mode`() = runTest {
        gameRepository.createGameResult = Result.success(testGame(id = "bot-game"))
        val events = collectEvents()

        viewModel.createGame(GameMode.BOT)

        assertEquals(listOf(MenuEvent.GameCreated("bot-game")), events)
        assertEquals(GameMode.BOT, gameRepository.lastCreatedMode)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `creating human game uses human mode`() = runTest {
        gameRepository.createGameResult = Result.success(testGame(id = "human-game"))
        val events = collectEvents()

        viewModel.createGame(GameMode.HUMAN)

        assertEquals(listOf(MenuEvent.GameCreated("human-game")), events)
        assertEquals(GameMode.HUMAN, gameRepository.lastCreatedMode)
    }

    @Test
    fun `create game failure shows error`() = runTest {
        gameRepository.createGameResult = Result.failure(ApiException(500, "Ошибка сервера"))
        val events = collectEvents()

        viewModel.createGame(GameMode.BOT)

        assertEquals(listOf(MenuEvent.ShowError("Ошибка сервера")), events)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `create game unauthorized emits session expired`() = runTest {
        gameRepository.createGameResult = Result.failure(ApiException(401, "unauthorized"))
        val events = collectEvents()

        viewModel.createGame(GameMode.BOT)

        assertEquals(listOf(MenuEvent.SessionExpired("unauthorized")), events)
    }

    @Test
    fun `create game while loading is ignored`() = runTest {
        gameRepository.createGameResult = Result.success(testGame(id = "first"))
        val gate = CompletableDeferred<Unit>()
        gameRepository.createGameGate = gate
        val events = collectEvents()

        viewModel.createGame(GameMode.BOT)
        viewModel.createGame(GameMode.BOT)
        gate.complete(Unit)

        assertEquals(listOf(MenuEvent.GameCreated("first")), events)
        assertEquals(1, events.size)
    }

    @Test
    fun `logout clears session and emits event`() = runTest {
        val events = collectEvents()

        viewModel.logout()

        assertTrue(authRepository.logoutCalled)
        assertEquals(listOf<MenuEvent>(MenuEvent.LoggedOut), events)
        assertFalse(viewModel.state.value.isLoading)
    }
}
