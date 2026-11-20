package com.example.tiktak_app.presentation

import com.example.tiktak_app.OPPONENT_ID
import com.example.tiktak_app.USER_ID
import com.example.tiktak_app.domain.model.Game
import com.example.tiktak_app.domain.model.Game.Companion.BOT_ID
import com.example.tiktak_app.presentation.mapper.toViewData
import com.example.tiktak_app.testGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameViewDataTest {

    @Test
    fun `status text describes win loss draw and turns`() {
        val finished = { winner: String?, status: String ->
            testGame(status = status, winner = winner).toViewData()
        }

        assertEquals(
            "Победа",
            finished(USER_ID, Game.STATUS_FINISHED).statusText(USER_ID)
        )
        assertEquals(
            "Поражение",
            finished(OPPONENT_ID, Game.STATUS_FINISHED).statusText(USER_ID)
        )
        assertEquals("Ничья", finished(null, Game.STATUS_DRAW).statusText(USER_ID))
        assertEquals(
            "Ожидание игроков",
            testGame(status = Game.STATUS_WAITING).toViewData().statusText(USER_ID)
        )
        assertEquals(
            "Ваш ход",
            testGame(currentPlayer = USER_ID).toViewData().statusText(USER_ID)
        )
        assertEquals(
            "Ходит соперник",
            testGame(currentPlayer = OPPONENT_ID).toViewData().statusText(USER_ID)
        )
    }

    @Test
    fun `player label resolves me bot and opponent`() {
        val game = testGame().toViewData()

        assertEquals("Вы", game.playerLabel(USER_ID, USER_ID))
        assertEquals("Компьютер", game.playerLabel(BOT_ID, USER_ID))
        assertEquals("Соперник", game.playerLabel(OPPONENT_ID, USER_ID))
        assertEquals("Ожидание игрока", game.playerLabel(null, USER_ID))
    }

    @Test
    fun `symbol and turn are read from view data`() {
        val board = listOf(
            listOf(Game.SYMBOL_X, Game.EMPTY_CELL, Game.EMPTY_CELL),
            listOf(Game.EMPTY_CELL, Game.EMPTY_CELL, Game.EMPTY_CELL),
            listOf(Game.EMPTY_CELL, Game.EMPTY_CELL, Game.SYMBOL_O)
        )
        val game = testGame(board = board, currentPlayer = USER_ID).toViewData()

        assertEquals("X", game.symbolAt(0, 0))
        assertEquals("O", game.symbolAt(2, 2))
        assertEquals("", game.symbolAt(1, 1))
        assertTrue(game.isMyTurn(USER_ID))
        assertFalse(game.isMyTurn(OPPONENT_ID))
        assertTrue(game.isCellEmpty(1, 1))
        assertFalse(game.isCellEmpty(0, 0))
    }
}
