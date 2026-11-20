package com.example.tiktak_app.presentation.viewmodel

import com.example.tiktak_app.FakeAuthRepository
import com.example.tiktak_app.FakeGameRepository
import com.example.tiktak_app.MainDispatcherRule
import com.example.tiktak_app.OPPONENT_ID
import com.example.tiktak_app.USER_ID
import com.example.tiktak_app.domain.model.Game
import com.example.tiktak_app.domain.repository.ApiException
import com.example.tiktak_app.testGame
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
class GamesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var gameRepository: FakeGameRepository
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: GamesViewModel

    @Before
    fun setUp() {
        gameRepository = FakeGameRepository()
        authRepository = FakeAuthRepository()
        authRepository.currentUser = testUser(id = USER_ID)
        viewModel = GamesViewModel(gameRepository, authRepository)
    }

    private fun TestScope.collectEvents(): MutableList<GamesEvent> {
        val events = mutableListOf<GamesEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events += it }
        }
        return events
    }

    @Test
    fun `waiting games of other players are shown`() = runTest {
        gameRepository.waitingGamesResult = Result.success(
            listOf(
                testGame(id = "available", status = Game.STATUS_WAITING, playerX = OPPONENT_ID),
                testGame(id = "mine", status = Game.STATUS_WAITING, playerX = USER_ID),
                testGame(id = "active", status = Game.STATUS_IN_PROGRESS, playerX = OPPONENT_ID)
            )
        )

        viewModel.loadGames()

        assertEquals(listOf("available"), viewModel.state.value.items.map { it.id })
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `empty list produces empty state`() = runTest {
        gameRepository.waitingGamesResult = Result.success(emptyList())

        viewModel.loadGames()

        assertEquals(emptyList<String>(), viewModel.state.value.items.map { it.id })
    }

    @Test
    fun `load failure shows error`() = runTest {
        gameRepository.waitingGamesResult =
            Result.failure(ApiException(500, "Сервер недоступен"))
        val events = collectEvents()

        viewModel.loadGames()

        assertEquals(listOf(GamesEvent.ShowError("Сервер недоступен")), events)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `unauthorized load emits session expired`() = runTest {
        gameRepository.waitingGamesResult =
            Result.failure(ApiException(401, "unauthorized"))
        val events = collectEvents()

        viewModel.loadGames()

        assertEquals(listOf(GamesEvent.SessionExpired("unauthorized")), events)
    }

    @Test
    fun `repeated load while loading is ignored`() = runTest {
        gameRepository.waitingGamesResult = Result.success(emptyList())

        viewModel.loadGames()
        viewModel.loadGames()

        assertEquals(emptyList<String>(), viewModel.state.value.items.map { it.id })
    }
}
