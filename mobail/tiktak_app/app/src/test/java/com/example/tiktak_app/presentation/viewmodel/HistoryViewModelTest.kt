package com.example.tiktak_app.presentation.viewmodel

import com.example.tiktak_app.FakeGameRepository
import com.example.tiktak_app.MainDispatcherRule
import com.example.tiktak_app.domain.model.FinishedGame
import com.example.tiktak_app.domain.repository.ApiException
import com.example.tiktak_app.testFinishedGame
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
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var gameRepository: FakeGameRepository
    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setUp() {
        gameRepository = FakeGameRepository()
        viewModel = HistoryViewModel(gameRepository)
    }

    private fun TestScope.collectEvents(): MutableList<HistoryEvent> {
        val events = mutableListOf<HistoryEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events += it }
        }
        return events
    }

    @Test
    fun `history items keep server order`() = runTest {
        gameRepository.finishedGamesResult = Result.success(
            listOf(
                testFinishedGame(gameId = "newest", result = FinishedGame.RESULT_WIN),
                testFinishedGame(gameId = "older", result = FinishedGame.RESULT_LOSS),
                testFinishedGame(gameId = "oldest", result = FinishedGame.RESULT_DRAW)
            )
        )

        viewModel.load()

        assertEquals(
            listOf("newest", "older", "oldest"),
            viewModel.state.value.items.map { it.gameId }
        )
        assertEquals(
            listOf("Победа", "Поражение", "Ничья"),
            viewModel.state.value.items.map { it.resultText }
        )
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `empty history produces empty state`() = runTest {
        gameRepository.finishedGamesResult = Result.success(emptyList())

        viewModel.load()

        assertEquals(0, viewModel.state.value.items.size)
    }

    @Test
    fun `history failure shows error`() = runTest {
        gameRepository.finishedGamesResult =
            Result.failure(ApiException(500, "Сервер недоступен"))
        val events = collectEvents()

        viewModel.load()

        assertEquals(listOf(HistoryEvent.ShowError("Сервер недоступен")), events)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `history unauthorized emits session expired`() = runTest {
        gameRepository.finishedGamesResult =
            Result.failure(ApiException(401, "unauthorized"))
        val events = collectEvents()

        viewModel.load()

        assertEquals(listOf(HistoryEvent.SessionExpired("unauthorized")), events)
    }
}
