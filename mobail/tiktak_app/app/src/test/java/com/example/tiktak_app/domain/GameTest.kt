package com.example.tiktak_app.domain

import com.example.tiktak_app.OPPONENT_ID
import com.example.tiktak_app.USER_ID
import com.example.tiktak_app.domain.model.Game
import com.example.tiktak_app.emptyBoard
import com.example.tiktak_app.testGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameTest {

    @Test
    fun `game is finished for finished and draw statuses`() {
        assertTrue(testGame(status = Game.STATUS_FINISHED).isFinished)
        assertTrue(testGame(status = Game.STATUS_DRAW).isFinished)
        assertFalse(testGame(status = Game.STATUS_WAITING).isFinished)
        assertFalse(testGame(status = Game.STATUS_IN_PROGRESS).isFinished)
    }

    @Test
    fun `turn belongs to current player only during active game`() {
        assertTrue(testGame(currentPlayer = USER_ID).isMyTurn(USER_ID))
        assertFalse(testGame(currentPlayer = USER_ID).isMyTurn(OPPONENT_ID))
        assertFalse(testGame(currentPlayer = null).isMyTurn(USER_ID))
        assertFalse(
            testGame(status = Game.STATUS_FINISHED, currentPlayer = USER_ID)
                .isMyTurn(USER_ID)
        )
        assertFalse(testGame(currentPlayer = USER_ID).isMyTurn(null))
    }

    @Test
    fun `cell returns value or empty for out of bounds`() {
        val board = listOf(
            listOf(Game.SYMBOL_X, Game.EMPTY_CELL, Game.EMPTY_CELL),
            listOf(Game.EMPTY_CELL, Game.SYMBOL_O, Game.EMPTY_CELL),
            listOf(Game.EMPTY_CELL, Game.EMPTY_CELL, Game.EMPTY_CELL)
        )
        val game = testGame(board = board)

        assertEquals(Game.SYMBOL_X, game.cell(0, 0))
        assertEquals(Game.SYMBOL_O, game.cell(1, 1))
        assertEquals(Game.EMPTY_CELL, game.cell(2, 2))
        assertEquals(Game.EMPTY_CELL, game.cell(-1, 0))
        assertEquals(Game.EMPTY_CELL, game.cell(5, 5))
        assertTrue(game.isCellEmpty(2, 2))
        assertFalse(game.isCellEmpty(0, 0))
    }

    @Test
    fun `symbol of user depends on player side`() {
        val game = testGame(
            playerX = USER_ID,
            playerO = OPPONENT_ID,
            board = emptyBoard()
        )

        assertEquals(Game.SYMBOL_X, game.symbolOf(USER_ID))
        assertEquals(Game.SYMBOL_O, game.symbolOf(OPPONENT_ID))
        assertEquals(Game.EMPTY_CELL, game.symbolOf("unknown"))
        assertEquals(Game.EMPTY_CELL, game.symbolOf(null))
    }

    @Test
    fun `bot game is detected by mode`() {
        assertTrue(testGame(mode = Game.MODE_BOT).isBotGame)
        assertFalse(testGame(mode = Game.MODE_HUMAN).isBotGame)
    }
}
