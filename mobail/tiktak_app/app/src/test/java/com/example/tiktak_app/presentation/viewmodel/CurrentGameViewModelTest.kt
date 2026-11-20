package com.example.tiktak_app.presentation.viewmodel

import com.example.tiktak_app.FakeAuthRepository
import com.example.tiktak_app.FakeGameRepository
import com.example.tiktak_app.FakeGameSession
import com.example.tiktak_app.MainDispatcherRule
import com.example.tiktak_app.OPPONENT_ID
import com.example.tiktak_app.USER_ID
import com.example.tiktak_app.domain.model.Game
import com.example.tiktak_app.domain.repository.ApiException
import com.example.tiktak_app.domain.repository.GameEvent
import com.example.tiktak_app.emptyBoard
import com.example.tiktak_app.testGame
import com.example.tiktak_app.testUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CurrentGameViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var gameRepository: FakeGameRepository
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: CurrentGameViewModel
    private lateinit var session: FakeGameSession

    @Before
    fun setUp() {
        gameRepository = FakeGameRepository()
        authRepository = FakeAuthRepository()
        authRepository.currentUser = testUser(id = USER_ID)
        session = FakeGameSession()
        gameRepository.openGameResult = Result.success(session)
        viewModel = CurrentGameViewModel(gameRepository, authRepository)
    }

    private fun TestScope.collectEvents(): MutableList<CurrentGameEvent> {
        val events = mutableListOf<CurrentGameEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events += it }
        }
        return events
    }

    @Test
    fun `open game passes id and join flag to repository`() = runTest {
        viewModel.start("game-1", join = true)

        assertEquals("game-1" to true, gameRepository.lastOpenArgs)
        assertEquals(USER_ID, viewModel.state.value.currentUserId)
    }

    @Test
    fun `blank game id does not open socket`() = runTest {
        viewModel.start("", join = true)

        assertNull(gameRepository.lastOpenArgs)
    }

    @Test
    fun `game update from socket is rendered`() = runTest {
        viewModel.start("game-1", join = false)

        session.emit(GameEvent.GameUpdated(testGame(id = "game-1", status = Game.STATUS_WAITING)))

        assertEquals("game-1", viewModel.state.value.game?.id)
        assertEquals(Game.STATUS_WAITING, viewModel.state.value.game?.status)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `open failure emits error`() = runTest {
        gameRepository.openGameResult = Result.failure(ApiException(500, "Нет связи"))
        val events = collectEvents()

        viewModel.start("game-1", join = true)

        assertEquals(listOf(CurrentGameEvent.ShowError("Нет связи")), events)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `missing user emits session expired`() = runTest {
        authRepository.currentUser = null
        val events = collectEvents()

        viewModel.start("game-1", join = true)

        assertEquals(
            listOf(CurrentGameEvent.SessionExpired("Сессия истекла, войдите заново")),
            events
        )
        assertNull(gameRepository.lastOpenArgs)
    }

    @Test
    fun `server error after rejected move is shown in human game`() = runTest {
        val events = collectEvents()
        viewModel.start("game-1", join = false)
        session.emit(GameEvent.GameUpdated(testGame(currentPlayer = OPPONENT_ID)))

        viewModel.makeMove(row = 0, col = 0)
        session.emit(GameEvent.Error("Недопустимый ход"))

        assertEquals(listOf(CurrentGameEvent.ShowError("Недопустимый ход")), events)
    }

    @Test
    fun `server error after rejected move is shown in bot game`() = runTest {
        val events = collectEvents()
        viewModel.start("game-1", join = false)
        session.emit(
            GameEvent.GameUpdated(
                testGame(
                    mode = Game.MODE_BOT,
                    currentPlayer = Game.BOT_ID,
                    playerO = Game.BOT_ID
                )
            )
        )

        viewModel.makeMove(row = 0, col = 0)
        session.emit(GameEvent.Error("Щас вроде противник должен ходить"))

        assertEquals(
            listOf(CurrentGameEvent.ShowError("Щас вроде противник должен ходить")),
            events
        )
    }

    @Test
    fun `socket token error emits session expired`() = runTest {
        val events = collectEvents()
        viewModel.start("game-1", join = false)

        session.emit(GameEvent.Error("Некорректный токен"))

        assertEquals(listOf(CurrentGameEvent.SessionExpired("Некорректный токен")), events)
    }

    @Test
    fun `move sends board with my symbol in selected cell`() = runTest {
        viewModel.start("game-1", join = false)
        session.emit(
            GameEvent.GameUpdated(
                testGame(currentPlayer = USER_ID, playerX = USER_ID, playerO = OPPONENT_ID)
            )
        )

        viewModel.makeMove(row = 1, col = 2)

        val board = session.lastBoard
        assertEquals(Game.SYMBOL_X, board?.get(1)?.get(2))
        assertEquals(Game.EMPTY_CELL, board?.get(0)?.get(0))
        assertEquals(9, board?.flatten()?.size)
    }

    @Test
    fun `move is sent when it is not my turn`() = runTest {
        viewModel.start("game-1", join = false)
        session.emit(GameEvent.GameUpdated(testGame(currentPlayer = OPPONENT_ID)))

        viewModel.makeMove(row = 0, col = 0)

        assertEquals(Game.SYMBOL_X, session.lastBoard?.get(0)?.get(0))
    }

    @Test
    fun `move is sent while bot is thinking`() = runTest {
        viewModel.start("game-1", join = false)
        session.emit(
            GameEvent.GameUpdated(
                testGame(
                    mode = Game.MODE_BOT,
                    currentPlayer = Game.BOT_ID,
                    playerO = Game.BOT_ID
                )
            )
        )

        viewModel.makeMove(row = 2, col = 2)

        assertEquals(Game.SYMBOL_X, session.lastBoard?.get(2)?.get(2))
    }

    @Test
    fun `move is sent for occupied cell`() = runTest {
        val board = listOf(
            listOf(Game.SYMBOL_O, Game.EMPTY_CELL, Game.EMPTY_CELL),
            listOf(Game.EMPTY_CELL, Game.EMPTY_CELL, Game.EMPTY_CELL),
            listOf(Game.EMPTY_CELL, Game.EMPTY_CELL, Game.EMPTY_CELL)
        )
        viewModel.start("game-1", join = false)
        session.emit(
            GameEvent.GameUpdated(
                testGame(board = board, currentPlayer = USER_ID, playerX = USER_ID)
            )
        )

        viewModel.makeMove(row = 0, col = 0)

        assertEquals(Game.SYMBOL_X, session.lastBoard?.get(0)?.get(0))
        assertEquals(Game.SYMBOL_O, viewModel.state.value.game?.board?.get(0)?.get(0))
    }

    @Test
    fun `move is ignored before first game state`() = runTest {
        viewModel.start("game-1", join = false)

        viewModel.makeMove(row = 0, col = 0)

        assertNull(session.lastBoard)
    }

    @Test
    fun `move is sent after game finished`() = runTest {
        viewModel.start("game-1", join = false)
        session.emit(
            GameEvent.GameUpdated(
                testGame(
                    board = emptyBoard(),
                    status = Game.STATUS_FINISHED,
                    currentPlayer = null,
                    winner = USER_ID
                )
            )
        )

        viewModel.makeMove(row = 0, col = 0)

        assertEquals(Game.SYMBOL_X, session.lastBoard?.get(0)?.get(0))
    }

    @Test
    fun `stop closes socket session`() = runTest {
        viewModel.start("game-1", join = false)

        viewModel.stop()

        assertTrue(session.closed)
    }
}
