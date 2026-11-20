package com.example.tiktak_app.presentation.viewmodel

import com.example.tiktak_app.FakeGameRepository
import com.example.tiktak_app.MainDispatcherRule
import com.example.tiktak_app.domain.repository.ApiException
import com.example.tiktak_app.testLeaderboardPlayer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var defaultLocale: Locale
    private lateinit var gameRepository: FakeGameRepository
    private lateinit var viewModel: LeaderboardViewModel

    @Before
    fun setUp() {
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        gameRepository = FakeGameRepository()
        viewModel = LeaderboardViewModel(gameRepository)
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    private fun TestScope.collectEvents(): MutableList<LeaderboardEvent> {
        val events = mutableListOf<LeaderboardEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events += it }
        }
        return events
    }

    @Test
    fun `positions are assigned from one in server order`() = runTest {
        gameRepository.leaderboardResult = Result.success(
            listOf(
                testLeaderboardPlayer(userId = "a", login = "first", winsRatio = 9.0),
                testLeaderboardPlayer(userId = "b", login = "second", winsRatio = 4.0),
                testLeaderboardPlayer(userId = "c", login = "third", winsRatio = 1.0)
            )
        )

        viewModel.load()

        val items = viewModel.state.value.items
        assertEquals(listOf(1, 2, 3), items.map { it.position })
        assertEquals(listOf("first", "second", "third"), items.map { it.login })
        assertEquals(listOf("9.00", "4.00", "1.00"), items.map { it.winsRatio })
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `empty leaderboard produces empty state`() = runTest {
        gameRepository.leaderboardResult = Result.success(emptyList())

        viewModel.load()

        assertEquals(0, viewModel.state.value.items.size)
    }

    @Test
    fun `leaderboard failure shows error`() = runTest {
        gameRepository.leaderboardResult =
            Result.failure(ApiException(500, "Сервер недоступен"))
        val events = collectEvents()

        viewModel.load()

        assertEquals(
            listOf(LeaderboardEvent.ShowError("Сервер недоступен")),
            events
        )
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `leaderboard unauthorized emits session expired`() = runTest {
        gameRepository.leaderboardResult =
            Result.failure(ApiException(401, "unauthorized"))
        val events = collectEvents()

        viewModel.load()

        assertEquals(listOf(LeaderboardEvent.SessionExpired("unauthorized")), events)
    }
}
